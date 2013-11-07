# Searchzy

Searchzy is Centzy's search service.  It employs ElasticSearch under
the hood.

Searchzy is implemented in Clojure, to take advantage of native
transport between the JVM processes.


## Prerequisites


### Leiningen

You will need [Leiningen][1] 1.7.0 or above installed.

[1]: https://github.com/technomancy/leiningen

Leiningen is magic.  It's a tool for managing your project's
dependencies, running your code, and producing binary deliverables.


### ElasticSearch

Searchzy is not itself a search engine.  Instead, Searchzy knows all
about Centzy data, how to add that data into a search engine, and to
query against it.  The search engine it uses under the hood is
ElasticSearch.

In order to use Searchzy, either during indexing or at query time, you
will need ElasticSearch installed and running.

Once ElasticSearch is installed, run it thus:

    elasticsearch -f -D es.config=/usr/local/opt/elasticsearch/config/elasticsearch.yml

Then, you must tell Searchzy where to find ElasticSearch.  You can do
so by modifying this file:

    src/searchzy/cfg.clj

(This will probably change to a Searchzy command-line flag.)



### MongoDB

The data over which Searchzy searches comes from MongoDB.  At indexing
time, you must have MongoDB running.

To run MongoDB:
    
    mongod

You must also tell Searchzy where to find MongoDB.  You can do so by
modifying this file:

    src/searchzy/cfg.clj

(Again, this will probably change to a Searchzy command-line flag.)



## Running

To index, run this command:

    lein run

Indexing currently takes just over 1 hour (on my MacBook Air laptop).

To start the search service, run:

    lein ring server



## License

Copyright © 2013 Centzy
