(ns yamfood.core.db.core)

; TODO: Read from env
(def uri "jdbc:postgresql://ec2-54-225-205-79.compute-1.amazonaws.com:5432/d57fagasd6m0sk?user=rpvpfpevaeacui&password=6825de3447090f4caf0f79ea7bd80a0b5f3bdc84bb0abc298421f8928cf89681&sslmode=require")
(def db {:classname      "org.postgresql.Driver"
         :connection-uri uri})
