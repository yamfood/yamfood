(ns yamfood.tasks.iiko
  (:require
    [clojure.set :as set]
    [clojure.java.jdbc :as jdbc]
    [yamfood.core.db.core :as db]
    [honeysql.core :as hs]
    [yamfood.integrations.iiko.core :as iiko]
    [clojure.tools.logging :as log]))


(defn sync-disabled-products []
  (jdbc/with-db-transaction
    [t-con db/db]
    (let [;; new yamfood disabled products grouped -by-terminal
          disabled-products-by-terminal (->> (iiko/stop-list!)
                                             (map (fn [{:keys [deliveryTerminalId items]}]
                                                    {deliveryTerminalId (set (map :productId items))}))
                                             ;; [{"terminal-uuid-1" #{"product-uuid-1", "product-uuid-2", ...}}, {"terminal-uuid-2" #{"product-uuid-3", ...}}, ...]
                                             (apply merge-with set/union)

                                             ;; override iiko-ids with yamfood product-ids, removing not existent
                                             (medley.core/map-vals
                                               (fn [iiko-ids]
                                                 (->> {:select [:id]
                                                       :from   [:products]
                                                       :where  [:str-in
                                                                (hs/call :->> :payload "iiko_id")
                                                                ;; TODO partition for big array
                                                                (vec iiko-ids)]}
                                                      (hs/format)
                                                      (jdbc/query db/db)
                                                      (map :id))))
                                             ;; remove empty terminals
                                             (medley.core/filter-vals seq))
          ;; all kitchens that have terminal from stop list grouped -by-terminal
          kitchens-by-terminal (->> {:select [:id [(hs/call :->> :payload "deliveryTerminalId") :terminal_id]]
                                     :from   [:kitchens]
                                     :where  [:and [:str-in
                                                    (hs/call :->> :payload "deliveryTerminalId")
                                                    ;; TODO partition for big array
                                                    (keys disabled-products-by-terminal)]]}
                                    (hs/format)
                                    (jdbc/query db/db)
                                    (group-by :terminal_id)
                                    (medley.core/map-vals (partial map :id)))]

      (jdbc/delete!
        t-con
        "disabled_products"
        ["TRUE"])

      (->> kitchens-by-terminal
           (reduce (fn [acc [terminal-id kitchen-ids]]
                     (concat
                       acc
                       ;; cross join all products with kitchens, if their terminals are same
                       (for [kid kitchen-ids
                             pid (get disabled-products-by-terminal terminal-id)]
                         {:product_id pid
                          :kitchen_id kid})))
                   [])
           (jdbc/insert-multi! db/db :disabled_products)))))


(defn disabled-products-daemon!
  []
  (try
    (log/info "Starting disabled products synchronization...")
    (sync-disabled-products)
    (log/info "Disabled products synchronization finished.")
    (catch Exception e
      (println e))))
