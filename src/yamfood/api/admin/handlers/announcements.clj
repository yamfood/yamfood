(ns yamfood.api.admin.handlers.announcements
  (:require
    [yamfood.utils :as u]
    [compojure.core :as c]
    [clojure.spec.alpha :as s]
    [yamfood.api.pagination :as p]
    [yamfood.core.specs.core :as cs]
    [yamfood.core.announcements.core :as a]))


(s/def ::text string?)
(s/def ::bot_id int?)
(s/def ::image_url string?)
(s/def ::send_at cs/timestamp?)


(defn announcements-list
  [request]
  (let [page (p/get-page request)
        per-page (p/get-per-page request)
        offset (p/calc-offset page per-page)
        count (a/all-announcements-count!)]
    {:body (p/format-result
             count
             per-page
             page
             (a/all-announcements!
               offset
               per-page))}))


(defn announcement-details
  [request]
  (let [announcement-id (u/str->int (:id (:params request)))
        announcement (a/announcement-by-id! announcement-id)]
    (if announcement
      {:body announcement}
      {:body   {:error "Announcement not found"}
       :status 404})))


(s/def ::create-announcement
  (s/keys :req-un [::text ::bot_id ::image_url]
          :opt-un [::send_at]))


(defn create-announcement
  [request]
  (let [body (:body request)
        valid? (s/valid? ::create-announcement body)]
    (if valid?
      (try
        {:body (a/create! body)}
        (catch Exception e
          {:body   {:error "Unexpected error"}
           :status 500}))
      {:body   {:error "Invalid input"}
       :status 400})))


(s/def ::patch-announcement
  (s/keys :opt-un [::text ::image_url ::send_at]))


(defn patch-announcement
  [request]
  (let [body (:body request)
        announcement-id (u/str->int (:id (:params request)))
        announcement (a/announcement-by-id! announcement-id)
        valid? (s/valid? ::create-announcement body)]
    (if (and valid? announcement)
      (try
        (do
          (a/update! announcement-id body)
          {:body (a/announcement-by-id! announcement-id)})
        (catch Exception e
          {:body   {:error "Unexpected error"}
           :status 500}))
      {:body   {:error "Invalid input"}
       :status 400})))


(defn delete-announcement
  [request]
  (let [announcement-id (u/str->int (:id (:params request)))
        announcement (a/announcement-by-id! announcement-id)]
    (if announcement
      (do
        (a/delete! announcement-id)
        {:status 204})
      {:body   {:error "Announcement not found"}
       :status 404})))


(c/defroutes
  routes
  (c/GET "/" [] announcements-list)
  (c/POST "/" [] create-announcement)

  (c/GET "/:id{[0-9]+}/" [] announcement-details)
  (c/PATCH "/:id{[0-9]+}/" [] patch-announcement)
  (c/DELETE "/:id{[0-9]+}/" [] delete-announcement))

