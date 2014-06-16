
  (let [path "/v1/suggestions"
        query "nai"
        size 8
        url1 (util/mk-url path {:query query
                                :address util/address
                                :miles util/miles
                                :size size})
        url2 (util/mk-url path {:query query
                                :lat util/lat
                                :lon util/lon})
        url3 (str url2 "&html=true")
        url4 (util/mk-url path {:query query :lat util/lat})]

# Suggestions (Auto-Suggest)
      
## Try It

First, try a couple of queries.  In the results, be sure to have a look at the `arguments`, to get a sense for how it's interpreting the query-string parameters.

### Well-formed Queries

Using `address` and specifying some optional values, `miles` and `size`:

* [/v1/suggestions/?
   query=nai &
   address=New%20York,%20NY &
   miles=2.5 &
   size=8
  ](http://localhost:3000/v1/suggestions?query=nai&address=New%20York,%20NY&miles=2.5&size=8)

Using `lat` and `lon` instead of `address:`

* [/v1/suggestions/?
   query=nai &
   lat=40.714 &
   lon=-74.006
  ](http://localhost:3000/v1/suggestions?query=nai&lat=40.714&lon=-74.006)

Same thing, but this time with `html` output:

* [/v1/suggestions/?
   query=nai &
   lat=40.714 &
   lon=-74.006 &
   html=t
  ](http://localhost:3000/v1/suggestions?query=nai&lat=40.714&lon=-74.006&html=t)

When there's no error, the response code is `200`.

### Errors

Try a malformed query (it's missing a `lon`), to see what an error looks like:

* [/v1/suggestions/?
   query=nai &
   lat=40.714
  ](http://localhost:3000/v1/suggestions?query=nai&lat=40.714)

If you look at the response headers, you can see that it returns a `404`:

`curl -I "http://localhost:3000/v1/suggestions?query=nai&lat=40.714"`


## Behavior

### Query

This is an auto-suggest query.  The final token of a user’s query is treated as a prefix.  For example, `hai` might be the beginning of the words "hair" or "haircut".
      
The search services performs prefix queries against three domains:

* BusinessCategory names - e.g. "Hair Salon"
* Item names - e.g. "Men's haircut"
* Business names - e.g. "Cinderella Hair Castle"
      

### Filtering

For Businesses, filtered by geographic proximity to the provided location.

BusinessCategory names and Item names are not filtered.
      
### Sorting

By ElasticSearch’s lexical sorting.
      
### Paging

Returns only the first 'page' of results for each domain, 5 by default.

### Format

The response is always JSON.  Within the JSON response, you may choose to have the data embedded as either:

* JSON data, -OR-
* an HTML string (to be injected into the DOM).
      
## Endpoint

The path: `/v1/suggestions`

It requires query string parameters to work correctly.
      
## Query String Parameters
      
### Output Format

* name: `html`
* type: boolean
  * true: any of `{true, t, 1}`
  * false: anything else
* optional: defaults to `false`
* purpose:
  * When `true`, outputs JSON with single attribute `html`, an HTML string ready to be injected into the DOM.
  * When `false`, outputs all data as JSON.
      
### Prefix Query

* name: `query`
* type: string
* required
* purpose: for searching against the names of:
  * BusinessCategories
  * Items
  * Businesses
* interpretation:
  * initial tokens are treated as complete words
  * the final token is treated as a prefix
  * treated as case-insensitive
