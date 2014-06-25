# Businesses  (Fix me)

## Try It

First, try a couple of queries.  In the results, be sure to have a
look at the `arguments` to get a sense for how it's interpreting the
query-string parameters.

Using `address` and specifying some optional values, `miles`, `from`,
and `size`:

  (let [path "/v1/lists"
        from 0
        size 8
        url1 (util/mk-url path {:address util/address
                                :miles util/miles
                                :from from :size size})
        url2 (util/mk-url path {:lat util/lat :lon util/lon})]

* [http://localhost:3000/v1/lists ?
   address=New%20York,%20NY &
   miles=2.5 &
   from=0 & size=8
  ](http://localhost:3000/v1/lists?address=New%20York,%20NY&miles=2.5&from=0&size=8)

Using `lat` and `lon` instead of `address:`

* [http://localhost:3000/v1/lists ?
   lat=40.714 &
   lon=-74.006
  ](http://localhost:3000/v1/lists?lat=40.714&lon=-74.006)

## Behavior
      
### Filtering

By geographical proximity to a geo-point.
      
### Paging

Only a subset of results is returned, based on provided offsets.
      
### Results

The returned data is simply Lists.
      
## Endpoint

The path: `/v1/lists`

It requires query-string parameters to work correctly.
      
## Query String Parameters

TODO: Add stuff here.
