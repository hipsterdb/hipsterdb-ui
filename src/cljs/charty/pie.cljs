(ns charty.pie
  (:require [reagent.core :as r]))

(defn interpol [cb]
  (let [current (atom nil)]
    (fn [d]
      (when (not @current) (reset! current d))
      (let [interpolate (.. js/d3 (interpolate @current d))]
        (reset! current (interpolate 0))
        (fn [t] (cb interpolate t))))))

(defn half [x] (/ x 2))

(defn mid-angle [d]
  (+ (.-startAngle d)
     (/ (- (.-endAngle d) (.-startAngle d)) 2)))

(defn lower-angle? [d]
  (< (mid-angle d) (.-PI js/Math)))

(defn translate [svg x y]
  (.. svg (attr "transform" (str "translate(" x "," y ")"))))

(defn mk-arc [radius outer inner]
  (.. (.arc (.-svg js/d3))
      (outerRadius (* radius outer))
      (innerRadius (* radius inner))))

(defn g [svg cls]
  (.. svg (append "g") (attr "class" cls)))


(comment
  [:polyline
   {:transition {:duration 1000
                 :tween {:attr  {:points (interpolate fn)}
                         :style {:text-anchor (interpol fn)}}}}]

  [:path {:style {:fill #(fill (.-index %))
                  :stroke #(fill (.-index %))}
          :d #(arc inner outer)
          :on {:mouse-over #(fade .1)
               :mouse-out #(fade 1)}}])

(def color
  (.. (.-scale js/d3)
      (ordinal)
      (range  #js["#98abc5", "#8a89a6", "#7b6888", "#6b486b", "#a05d56", "#d0743c", "#ff8c00"])))

(defn render-pie [{height :height width :width :as opts} data]
  (let [svg     (.. js/d3 (select "svg") (append "g"))
        slices  (g svg "slices")
        labels  (g svg "labels")
        lines   (g svg "lines")

        radius (half (min width height))

        pie (.. (.pie (.-layout js/d3))
                (sort nil)
                (value (fn [x] (.-value x))))

        arc       (mk-arc radius 0.8 0.4)
        outerArc  (mk-arc radius 0.9 0.9)

        _ (translate svg (half width) (half height))

        key (fn [x] (.. x -data -label))

        update (fn [data]
                 (doto (.. slices (selectAll "path.slice") (data (pie data) key))
                   (.. (enter)
                       (insert "path")
                       (attr "class" "slice")
                       (style "fill" (fn [d] (color (.. d -data -label)))))
                   (.. (transition) (duration 200)
                       (attrTween "d" (interpol (fn [inter t] (arc (inter t))))))
                   (.. (exit) (remove)))

                 (doto (.. labels (selectAll "text") (data (pie data) key))
                   (.. (enter) (append "text") (attr "dy" ".35em") (text key))
                   (.. (transition) (duration 200)
                       (attrTween "transform"
                                  (interpol (fn [inter t]
                                              (let [d2 (inter t)
                                                    pos (.. outerArc (centroid d2))]
                                                (aset pos 0 (* radius (if (lower-angle? d2) 1 -1)))
                                                (str "translate(" pos ")")))))

                       (styleTween "text-anchor" (interpol (fn [inter t] (if (lower-angle? (inter t)) "start" "end")))))
                   (.. (exit) (remove)))

                 (doto (.. lines (selectAll "polyline") (data (pie data) key))
                   (.. (enter) (append "polyline"))
                   (.. (transition) (duration 200)
                       (attrTween "points"
                                  (interpol (fn [inter t]
                                              (let [d2 (inter t)
                                                    pos (.. outerArc (centroid d2))]
                                                (aset pos 0 (* 0.95 radius (if (lower-angle? d2) 1 -1)))
                                                #js[(.. arc (centroid d2))
                                                    (.. outerArc (centroid d2))
                                                    pos])))))
                   (..  (exit) (remove))))]
    (update data)
    nil))

(defn pie [opts data]
  (r/create-class
   {:reagent-render (fn [] [:div
                            [:style " path.slice{ stroke-width:2px;} .labels text {fill: white;} polyline{opacity: .3; stroke: white; stroke-width: 2px; fill: none;} "]
                            [:svg {:width (:width opts)
                                        :height (:height opts)}]])

    :component-did-mount (fn []
                           (let [d3data (clj->js data)]
                             (render-pie opts d3data)))

    :component-did-update (fn [this]
                            (let [[_ _ data] (r/argv this)
                                  d3data (clj->js data)]
                              (render-pie opts d3data)))}))
