(ns pgtron.layout
  (:require [gardner.core :as css]
            [pgtron.style :refer [style icon] :as st]))

(defn navigation [glob]
  [:div#nav
   (style [:#nav
           {:border-bottom "1px solid #262626"
            :$color [:gray :bg-1]}
           [:.brand {:$margin [0 1]}]
           [:.item {:display "inline-block"
                    :$padding 1}]
           [:.logo {:$height 2.5}]])

   [:a.brand {:href "#/"} [:img.logo {:src "img/logo.png"}]]
   (for [x (or (:bread-crump glob) [])]
     [:a.item {:key (:title x)
               :href "#/"}
      (when-let [ic (:icon x)] (icon ic)) " " (:title x)])])

(defn footer []
  [:div#footer
   (style [:#footer {:$padding 1
                     :$color [:gray :bg-1]
                     :border-top "1px solid #262626"
                     :$absolute [nil 0 0 0]}])
   "footer"])

(defn layout [glob cnt]
  [:div#layout
   (st/main-style)
   [navigation glob]
   (style [:#center {:$padding 1}])
   [:div#center cnt]])


