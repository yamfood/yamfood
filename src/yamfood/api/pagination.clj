(ns yamfood.api.pagination
  (:require
    [yamfood.utils :as u]))


(defn calc-offset
  [page per-page]
  (* (- page 1) per-page))


(defn get-page
  [request]
  (let [default (str 1)
        params (:params request)]
    (u/str->int (get params "page" default))))


(defn get-per-page
  [request]
  (let [default (str 100)
        params (:params request)]
    (u/str->int (get params "per_page" default))))


(defn format-result
  [count per-page page data]
  {:pages (+ 1 (int (/ count per-page)))
   :page  page
   :data  data})

