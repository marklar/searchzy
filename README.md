# searchzy

Search
Search serviceClojure service using ElasticSearch.

## Prerequisites

You will need [Leiningen][1] 1.7.0 or above installed.

[1]: https://github.com/technomancy/leiningen

You will need MongoDB and ElasticSearch.
For indexing, both must be running.
For querying, only ElasticSearch is required.

Modify the configuration information in src/searchzy/cfg.clj
to match that of MongoDB and ElasticSearch.

ElasticSearch:

    elasticsearch -f -D es.config=/usr/local/opt/elasticsearch/config/elasticsearch.yml

MongoDB:
    
    mongod


## Running

To index, run:

    lein run

To start a web server for the search service, run:

    lein ring server

## License

Copyright © 2013 FIXME
