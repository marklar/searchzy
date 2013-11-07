# Searchzy

Searchzy is Centzy's search service.  It employs ElasticSearch under
the hood.

Searchzy is implemented in Clojure, to take advantage of native
transport between the JVM processes.


## Prerequisites

### Java

Both ElasticSearch and Searchzy run on the JVM, so you'll need to have
Java installed.


### Leiningen

You will need [Leiningen][1] 1.7.0 or above installed.

[1]: https://github.com/technomancy/leiningen

Leiningen is magic.  It's a tool for managing your project's
dependencies (including Clojure itself), running your code, producing
binary deliverables, and much more.


### ElasticSearch

Searchzy is not itself a search engine.  Instead, Searchzy knows all
about Centzy data, how to add that data into a search engine, and to
query against it.  The search engine it uses under the hood is
ElasticSearch.

In order to use Searchzy, either during indexing or at query time, you
will need ElasticSearch installed and running.

Once ElasticSearch is installed, run it thus:

    elasticsearch -f -D es.config=/usr/local/opt/elasticsearch/config/elasticsearch.yml

Then, you must tell Searchzy where to find ElasticSearch.

First, you need to find out your ElasticSearch's cluster name.
Retrieve it via ElasticSearch's REST API, thus:

    > curl http://localhost.com:9200/_cluster/nodes
    {"ok":true,"cluster_name":"elasticsearch_markwong-vanharen","nodes":...}}

Second, within this file:

    src/searchzy/cfg.clj

modify the values of this data structure:

    (def elastic-search-cfg
      {:cluster-name "elasticsearch_markwong-vanharen"
       :host "127.0.0.1"
       :port 9300})

You won't likely need to change the :port, as 9300 is the standard
port for ElasticSearch's binary transport.


### MongoDB

The data over which Searchzy searches comes from MongoDB.  At indexing
time, you must have MongoDB running.

To run MongoDB:
    
    mongod

You must also tell Searchzy where to find MongoDB.  You can do so by
modifying this file:

    src/searchzy/cfg.clj

Modify the values in this data structure:

    (def mongo-db-cfg
      {:db-name "centzy_web_production"
       :host "127.0.0.1"
       :port 27017})

to match your MongoDB setup.


## Running

To index, run this command:

    lein run

Indexing currently takes just over 1 hour (on my MacBook Air laptop).

To start the search service, run:

    lein ring server



## License

Copyright © 2013 Centzy
