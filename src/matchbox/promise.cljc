(ns matchbox.promise
  (:refer-clojure :exclude [promise -key -val map -deref])
  (:require
    [clojure.string :as str]
    [clojure.walk :as walk]
    [promesa.core :as prom]
    [matchbox.promise.protocols :as proto]
    [matchbox.utils :as utils])
  #?(:cljs (:require-macros [promesa.core :refer [alet]])))

;; this namespace should only be used with javascript (for now...)

(def promise prom/promise)

(def then prom/then)

(def catch prom/catch)

(def map prom/map)

(def chain prom/chain)

(def branch prom/branch)



