(ns pgtron.widgets
  (:require [pgtron.style :refer [style icon]]))

(defn table [data]
  [:div.data
   (style
    [:.data {:$color [:white :bg-1]
             :$text [0.8]
             :vertical-align "top"
             :clear "both"
             :float "left"
             :$margin [1 0]
             :$padding [1 2]}
     [:.columns {:$color [:white :bg-1]
                 :vertical-align "top"
                 :$margin [0 1 1 0]
                 :float "left"
                 :$padding [1 2]
                 :display "inline-block"}
      [:p.notes {:$margin [1 0 0 0]
                 :width "50em"
                 :$text [0.8]}
       [:b {:$color :orange}]]
      [:td.num {:text-align "right" :$color :blue}]
      [:th {:$color :gray}]
      [:.type {:$color :green}]
      [:.attr {:display "block"
               :$padding 0.1}]]])
   (let [rows data
         one (first rows)
         keys (and one (.keys js/Object one))]
     [:table.table-condensed
      [:thead
       [:tr
        (for [k keys] [:th {:key k} k])]]
      [:tbody
       (for [row data]
         [:tr {:key (.stringify js/JSON row)}
          (for [k keys]
            [:td.value {:key k :title k}
             (let [value (.stringify js/JSON (aget row k) nil " ")]
               (if (< (.-length value) 100)
                 value
                 (str (.substring value 0 100) "...")))])])]])])

(def tooltip-style
  [:.tt {:display "inline-block"
         :margin "0 0.5em"
         :width "20px"
         :height "20px"
         :line-height "20px"
         :cursor "pointer"
         :font-size "14px"
         :position "relative"
         :text-align "center"
         :border-radius "20%"
         :$color :blue}
    [:.tt-content
     {:position "absolute"
      :display "none"
      :box-shadow "0 0 4px gray"
      :border-radius "4px"
      :width "40em"
      :text-align "left"
      :z-index 1
      :$padding [1 2]
      :$color [:black :bg-note]
      :left "-30px"
      :top "30px"}]
   [:&:hover {:$color [:blue :black]}
     [:.tt-content {:display "block"}]]])

(defn tooltip [title content]
  [:span.tt (icon :question)
   [:div.tt-content content]])

(def block-style
  [:.block {:$color [:white :bg-1]
            :vertical-align "top"
            :$margin [0 1 1 0]
            :float "left"
            :$padding [1 2]
            :display "inline-block"}
   [:h3.block-title {:$margin [1 0]
                     :border-bottom "1px solid #666"
                     :$color :gray}]])

(defn block [title content]
  [:div.block
   [:h3.block-title title]
   [:div.block-content content]])

