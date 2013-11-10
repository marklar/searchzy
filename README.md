# Searchzy

Searchzy is Centzy's search service.  It employs ElasticSearch under
the hood.

Searchzy is implemented in Clojure, to take advantage of native
transport between the JVM processes.

## Contents

* [Prerequisites][1]
* [Configuration][2]
* [Running][3]
* [Deploying][4]

[1]: #prereqs
[2]: #config
[3]: #run
[4]: #deploy

## <a name="prereqs"></a>Prerequisites

### Java

Both ElasticSearch and Searchzy run on the JVM, so you'll need to have
Java installed.


### Leiningen

You will need [Leiningen][5] 1.7.0 or above installed.

[5]: https://github.com/technomancy/leiningen

Leiningen is magic.  Leiningen is your best friend.  It's like RVM and
Bundler and Rake all in one.  It's a tool for managing your project's
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

We must also tell Searchzy where to find ElasticSearch, but we'll
leave that for the "Configuration" section below.

### MongoDB

The data over which Searchzy searches comes from MongoDB.  At indexing
time, you must have MongoDB running.

To run MongoDB:

    mongod

Jast as with ElasticSearch, you must tell Searchzy where to find
MongoDB.  So let's get to the configuration step now.


## <a name="config"></a>Configuration

In the top-level directory of this project, add a file called:

    .config.yaml

Don't forget the leading period.

Its contents should look like this:

    api-key:
    mongo-db:
        host: 127.0.0.1
        port: 27017
        username:
        password:
        db-name: centzy_web_production
    elastic-search:
        host: 127.0.0.1
        port: 9300
        cluster-name: elasticsearch_something

Except that some of the values will need to add (e.g.: api-key) or
change.

In particular, you need to find out your ElasticSearch's cluster name.
Retrieve it via ElasticSearch's REST API, thus:

    > curl http://localhost.com:9200/_cluster/nodes
    {"ok":true,"cluster_name":"elasticsearch_something","nodes":...}}

You won't likely need to change the ports.  (27017 is the standard
MongoDB port, and 9300 is the standard port for ElasticSearch's binary
transport.)


## <a name="run"></a>Running

### Indexing

To index, run this command:

    lein run -m searchzy.index.core

Indexing currently takes just over 1 hour (on my MacBook Air laptop).

### Service

You may run Searchzy in either dev mode or prod mode.

#### Devevelopment mode

In dev mode, any Clojure code you modify gets automatically reloaded
with each server request.  This is very convenient for interactive
development.

    lein ring server

#### Production mode

In prod mode, your code doesn't get reloaded once the server is
launched.

There are two ways to run in prod mode.  The first requires that you
have leiningen installed whenever you want to start the server.

    lein run <PORT>

The second way allows you to create an 'uberjar' using leiningen:

    lein uberjar

and from that point forward just running the AOT-compiled Java bytecode
directly:

    java -jar target/searchzy-0.1.0-SNAPSHOT-standalone.jar <PORT>


## <a name="deploy"></a>Deploying

To deploy Searchzy to production machines, you will need two create
these two files:

    .config.yaml
    searchzy-0.1.0-SNAPSHOT-standalone.jar

To create .config.yaml, see [configuration][2].

To create the uberjar, run:

    lein uberjar

Once you have those two files, you must scp them to your production
machine.

Finally, from the same directory, start the server:

    java -jar searchzy-0.1.0-SNAPSHOT-standalone.jar <PORT>


## License

Copyright © 2013 Centzy
