(ns pgtron.database
  (:require-macros [cljs.core.async.macros :as m :refer [go alt!]])
  (:require [reagent.core :as r :refer [atom]]
            [pgtron.pg :as pg]

            [pgtron.table :as table]
            [pgtron.view :as view]
            [pgtron.proc :as proc]
            [pgtron.create :as create]
            [pgtron.query :as query]

            [charty.core :as chart]
            [cljs.core.async :refer [>! <!]]
            [pgtron.style :refer [style icon]]))

(def extensions-query
  "SELECT * FROM pg_extension")

(def tables-query "
  SELECT *
        , pg_size_pretty(pg_relation_size(c.oid)) AS size
        , pg_relation_size(c.oid) as raw_size AS size
   FROM pg_tables t
   JOIN pg_class c
     ON c.relname = t.tablename
  JOIN pg_namespace n
    ON n.oid = c.relnamespace AND t.schemaname = n.nspname
  WHERE t.schemaname NOT IN ('information_schema', 'pg_catalog')
  ")

(def schema-sql "
  select
   schema_name,
   (SELECT count(*) FROM pg_tables t WHERE t.schemaname = schema_name) AS tables_count,
   (select count(*) from information_schema.views v WHERE v.table_schema = schema_name) AS views_count
   FROM information_schema.schemata
   WHERE schema_name NOT IN ('pg_temp_1', 'pg_toast', 'pg_toast_temp_1')
   ORDER BY schema_name
  ")

(defn extensions []
  (let [state (atom {})]
    (pg/query-assoc extensions-query state [:items])
    (fn []
      [:div#extensions
       [:h4 "Extesions"]
       [:div.section
        [:div.box.new {:key "new"}
         [:a {:href (str  "#/new/extension")}
          [:h3 (icon :plus) "New extension"]]]
        (for [tbl (:items @state)]
          [:div.box {:key (.stringify js/JSON tbl nil " ")}
           [:h3 (.-extname tbl) " " (.-extversion tbl)]
           #_[:pre (.stringify js/JSON tbl nil " ")]])]])))

(defn schemas []
  (let [state (atom {})]
    (pg/query-assoc schema-sql state [:items])
    (fn []
      [:div#schemas
       [:h4 "Schemata"]
       [:div.section
        [:div.box.new {:key "new"}
         [:a {:href (str  "#/db/new/schema")}
          [:h3 (icon :plus)]
          [:div.details "Create Schema"]]]
        (for [tbl (:items @state)]
          [:a.box {:key (.stringify js/JSON tbl nil " ")
                   :href (str "#/db/schema/" (.-schema_name tbl))}
           [:h3 (.-schema_name tbl)]
           [:div.details
            [:span (.-tables_count tbl) " tables; "]
            [:span (.-views_count tbl) " views; "]]])]])))

(defn *tables [state]
  (go (let [res (<! (pg/exec tables-query))]
      (swap! state assoc :items
             (->> res
                  (group-by (fn [x] (.-schemaname x)))
                  (map (fn [[k v]] [k (sort-by #(.-tablename %)  v)]))
                  (into {}))))))

(defn tables []
  (let [state (atom {})]
    (*tables state)
    (fn []
      [:div#tables
       (style
        [:#tables
         [:.tbl {:display "inline-block"
                 :$width 25
                 :vertical-align "top"
                 :$margin 0.5
                 :$color [:gray :bg-1]
                 :$padding 1}
          [:h3 [:i {:$color [:gray]}]]
          [:.label {:$text [1 1.2 300 :center]
                    :$color :light-gray
                    :display "block"}]
          [:.details {:$text [0.8 1 :center]
                      :$padding 0.5}]]])

       (for [[sch tbls] (:items @state)]
         [:div {:key sch}
          [:h4 sch]
          [:div.section
           (for [tbl tbls]
             [:div.tbl {:key (.-tablename tbl)}
              [:a.label {:href (str "#/db/table/" (.-tablename tbl))} (.-tablename tbl)]
              [:div.details
               [:span.user [:a {:href (str  "#/users" (.-tableowner tbl))} "@" (.-tableowner tbl) " "]]
               "~" (.-reltuples tbl) " rows; "
               (.-size tbl)]
              #_[:pre (.stringify js/JSON tbl nil " ")]])]])])))

(defn $index [scope params]
  [:div#database
   (style [:#database
           {:$padding [1 2]}
           [:.section {:$margin [0 0 0 2]}]
           [:.box {:display "inline-block"
                   :border-top "6px solid #777"
                   :$width 25
                   :vertical-align "top"
                   :$margin 0.5
                   :$color [:light-gray :bg-1]
                   :$padding [0.5 1]}
            [:&:hover {:text-decoration "none"
                       :$color :white
                       :border-top "6px solid white"}
             [:h3 {:$color :white}]]
            [:.details {:$text [0.8 1 :center] :$padding 0.5}]
            [:&.new [:h3 {:$color :blue}]]
            [:h3 {:$color :light-gray
                  :$text [1 1.2 :center]}
             [:.fa {:$text [1.2 1.2 :bold] :$padding [0 1]}]]]

           [:.tbox {:display "inline-block"
                    :$padding [1 3]
                    :vertical-align "top"
                    :$width 18 
                    :$height 7 
                    :$margin 0.5
                    :border-top "6px solid #777"
                    :$color [:white :bg-1]}
            [:&:hover
             {:text-decoration "none"
              :border-top "6px solid white"}
             [:.fa {:$color :white}]
             [:h2 {:$color :white}]]
            [:.fa {:$text [2.5 2.5 :center]
                   :$color :light-gray
                   :display "block"}]
            [:h2 {:$text [1 1.5 :center]
                  :$color :light-gray
                  :$margin [0.5 0]}]
            [:.details {:$text [0.8 1 :center]}]
            [:&.template {:border-top "6px solid #777"}]]])

   [:div.section
    [:a.tbox {:href (str "#/query")}
     [icon :search]
     [:h2 "Queries"]]]

   [extensions]
   [schemas]
   [tables]])

(defn tables-sql [sch]
  (str 
   " SELECT t.tablename as display
    , *
    , pg_size_pretty(pg_relation_size(c.oid)) AS size
    , pg_relation_size(c.oid) as raw_size
   FROM pg_tables t
   JOIN pg_class c
     ON c.relname = t.tablename
  JOIN pg_namespace n
    ON n.oid = c.relnamespace AND t.schemaname = n.nspname
  WHERE t.schemaname = '" sch "' ORDER BY t.tablename"))

(defn views-sql [sch]
  (str "SELECT table_name as display, *
          FROM information_schema.views
         WHERE table_schema = '" sch "' ORDER by table_name"))

(defn procs-sql [sch q]
  (if (and q (not= q ""))
    (str
     "SELECT proname::text as display, *
     FROM pg_proc p
     JOIN pg_namespace n
       ON n.oid = p.pronamespace
    WHERE n.nspname = '" sch "'
     AND  proname ilike '%" q "%'
   ORDER BY proname
    LIMIT 300")
    (str
     "SELECT proname as display, *
     FROM pg_proc p
     JOIN pg_namespace n
       ON n.oid = p.pronamespace
    WHERE n.nspname = '" sch "'
   ORDER BY proname
    LIMIT 50")))

(defn bind [state path]
  (fn [ev]
    (swap! state assoc-in path (.. ev -target -value))))

(defn filter-str [q items]
  (for [i items]
    (do (aset i "hidden" (if (= q "") false (if (> (.indexOf (.-display i) q) -1) false true))) i)))


(defn schema-items [title ic xs href]
  (when (> (count (filter #(not (.-hidden %)) xs)) 0)
    [:div
     [:h3 title]
     [:div.col {:class (name ic)}
      (for [tbl xs]
        [:a.item {:key (.-display tbl)
                  :href (href tbl)
                  :class (when (.-hidden tbl) "hide")}
         [:span (icon ic) " " (.-display tbl)]])]]))

(defn $schema [scope {sch :schema :as params}]
  (let [state (atom {:search "" :tables [] :views [] :procs []})
        href (fn [tp id] (str "#/db/schema/" sch "/" tp "/" id))
        handle (fn [ev]
                 (let [q (.. ev -target -value)]
                   (pg/query-assoc (procs-sql sch q)  state [:procs])
                   (swap! state
                          (fn [old]
                            (-> old
                                (assoc :tables (filter-str q (:tables old)))
                                (assoc :views (filter-str q (:views old))))))))]
    (pg/query-assoc  (tables-sql sch) state [:tables])
    (pg/query-assoc  (views-sql sch)  state [:views])
    (pg/query-assoc  (procs-sql sch "")  state [:procs])
    (fn []
      [:div#schema
       (style [:#schema {:$padding [1 2]}
               [:h3 {:margin [2 0] :$text [0.9 1.5 :bold]
                     :color :gray
                     :border-bottom "1px solid #555"}]
               [:.hide {:display "none"}]
               [:.search [:input {:$text [1.1 2] :$padding 1}]]
               [:.col {:display "inline-block"
                       :vertical-align "top"
                       :$margin [0 1 1 0]}
                [:&.table [:.fa {:$color :blue}]]
                [:&.eye [:.fa {:$color :green}]]
                [:&.facebook [:.fa {:$color :orange}]]
                [:.item {:$padding [0.25 1]
                         :display "inline-block"
                         :$color :light-gray
                         :cursor "pointer"
                         :$text [1 1]
                         :$width 40}
                 [:&:hover {:$color [:white :black]
                            :text-decoration "none"}]
                 [:.fa {:$text [0.8 1]}]]]])
       [:div.search
        [:input.form-control {:placeholder "Search" :on-change handle}]]
       [:br]
       [:div#data
        [chart/pie {:width 800 :height 200}
         (map (fn [x] {:label (str (.-tablename x) " (" (.-size x) ")") :value (.-raw_size x)})
              (take 5 (sort-by #(- (.-raw_size %)) (:tables @state))))]
        (schema-items "Tables" :table (:tables @state) #(href "table" (.-display %)))
        (schema-items "Views" :eye (:views @state) #(href "view" (.-display %)))
        (schema-items "Functions" :facebook (:procs @state) #(href "proc" (.-display %)))]])))


(def routes
  {:GET #'$index
   "query" {:GET #'query/$index}
   "schema" {[:schema] {:GET #'$schema
                        "table" {[:table] {:GET #'table/$index}}
                        "proc"  {[:proc]   {:GET #'proc/$index}}
                        "view"  {[:view]   {:GET #'view/$index}}}}

   "tbl" {[:tbl] {:GET #'table/$index}}
   "new" #'create/routes})
