# Searchzy

Searchzy is Locality's search service.  It is a Locality-specific
wrapper around ElasticSearch.

Searchzy is implemented in Clojure, for speed.  ElasticSearch, which
runs on the JVM, supports both a REST API and a native-transport API,
the latter being much faster.  Since Clojure runs on the JVM, it may
take advantage of native transport.


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

What is Leiningen?  Leiningen is magic.  Leiningen is your best
friend.  It's like RVM and Bundler and Rake all in one.  It's a tool
for managing your project's dependencies (including Clojure itself),
running your code, producing binary deliverables, and much more.


### marklar/elastisch

It used to be necessary to use [our fork of Elastisch][10], but no
longer, as our enhancements have been integrated into the main repo.

[10]: https://github.com/marklar/elastisch

If you have a 'checkouts' directory (for using our fork), please
remove it:

    # from 'searchzy' root directory:
    > rm -rf checkouts


### ElasticSearch

Searchzy is not itself a search engine.  ElasticSearch is the search
engine, and Searchzy is a Locality-specific wrapper around it.
Searchzy knows all about Locality information, how to add that
information into a search engine, and to query against it.

In order to use Searchzy, either during indexing or at query time, you
need ElasticSearch installed and running.

Once ElasticSearch is installed, run it thus:

    > cd path/to/elasticsearch
    > bin/elasticsearch

We must also tell Searchzy where to find ElasticSearch, but we'll
leave that for the "Configuration" section below.

### MongoDB

The information over which Searchzy searches comes from MongoDB.  At
indexing time, you must have MongoDB running.

To run MongoDB:

    > mongod

Jast as with ElasticSearch, you must tell Searchzy where to find
MongoDB.  So let's get to the configuration step now.


## <a name="config"></a>Configuration

In the top-level directory of this project, add a file called:

    .config.yaml which can be created from .config.template.yaml

Don't forget the leading period.

Its contents should look like this:

```yaml
api-key:
geocoding:
    provider: bing  # -or- google
    bing-api-key: Al4K-FqvPXxTlsOZq3Hb1nXkmC93RyiSqszyzf2hXEVzzr8YsG6EeOS91j2H2iqh
    preferred-coords:
        "new york":
            lat: 40.757777777777775
            lon: -73.98583333333333
mongo-db:
    main:
        db-name: centzy2_development
        username:
        password: 
        host: 127.0.0.1
        port: 27017
    areas:
        db-name: locality_web_development_areas
        username:
        password:
        host: 127.0.0.1
        port: 27017
    businesses:
        db-name: locality_web_development_businesses
        username:
        password: 
        host: 127.0.0.1
        port: 27017
elastic-search:
    host: localhost
    port: 9300
    cluster-name: elasticsearch
```

Except that some of the values will need to be added (e.g.: api-key) or
changed.

For example, if you choose 'bing' as the geocoding provider, you must
provide a value for 'bing-api-key', or you'll get an Exception at
start-up time.  If you don't specify a geocoding provider, it defaults
to 'google', which does not require an API key.  (But you should use
'bing', as it has a much higher query limit.)

Also, you need to find out your ElasticSearch's cluster name. Retrieve
it via ElasticSearch's REST API, thus:

    > curl http://localhost:9200/_nodes/cluster_name
    {"cluster_name":"elasticsearch_something","nodes":...}}

You won't likely need to change the ports from the ones above.  (27017
is the standard MongoDB port, and 9300 is the standard port for
ElasticSearch's binary transport.)


## <a name="run"></a>Running

### Indexing

To index *all* domains, run this command:

    > lein run -m searchzy.index.core

Indexing currently all domains takes about 1 hour (on my MacBook Air
laptop).

To index just a subset of the domains, specify which.  For example:

    > lein run -m searchzy.index.core --domains "items biz-categories"

You can see the complete lists of options by using the help:

    > lein run -m searchzy.index.core --help

### Service

You may run Searchzy in either dev mode or prod mode.

#### Devevelopment mode

In dev mode, any Clojure code you modify gets automatically reloaded
with each server request.  This is very convenient for interactive
development.

    > lein ring server

#### Production mode

In prod mode, your code doesn't get reloaded once the server is
launched.

There are two ways to run in prod mode.  The first requires that you
have leiningen installed whenever you want to start the server.

    > lein run <PORT>

The second way allows you to create an 'uberjar' using leiningen:

    > lein uberjar

and from that point forward just running the AOT-compiled Java bytecode
directly:

    > java -jar target/searchzy-0.1.0-SNAPSHOT-standalone.jar <PORT>

## <a name="deploy"></a>Deploying

To deploy Searchzy to production machines, you will need two create
these two files:

    .config.yaml
    searchzy-0.1.0-SNAPSHOT-standalone.jar

To create .config.yaml, see [configuration][2].

To create the uberjar, run:

    > lein uberjar

Once you have those two files, you must scp them to your production
machine.

Finally, from the same directory, start the server:

    > java -jar searchzy-0.1.0-SNAPSHOT-standalone.jar <PORT>

## License

Copyright © 2013-2014 Locality
