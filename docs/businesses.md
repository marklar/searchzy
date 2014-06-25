# Searchzy Businesses
      
## Try It

First, try a couple of queries.  In the results, be sure to have a
look at the `arguments`, to get a sense for how it's interpreting the
query-string parameters.

Using `address` and specifying some optional values, `miles`, `from`,
and `size`:

* [/v1/businesses?
   query=nails &
   address=New%20York,%20NY &
   miles=2.5 &
   from=0 & size=8
  ](http://localhost:3000/v1/businesses?query=nails&address=New%20York,%20NY&miles=2.5&from=0&size=8)

Using `lat` and `lon` instead of `address`:

* [/v1/businesses?
   query=nails &
   lat=40.714 &
   lon=-74.006
  ](http://localhost:3000/v1/businesses?query=nails&lat=40.714&lon=-74.006)

## Behavior

### Query

The user's query (e.g. "curves") is interpreted as (part of) the name
of a Business, such as "Curves Gym".  The purpose of this endpoint is
to find those Businesses within a geographical area whose names are
the closest match to the query string.

This is not a prefix search.  It happens as a result of the user
submitting the form (i.e. clicking "search").
      
### Filtering

By geographical proximity to a geo-point.
      
### Sorting

Various options. By:

* ElasticSearch's score   -OR-
* Proximity   -OR-
* FIXME: "value_score_int", which is ElasticSearch’s score PLUS the highest value of any of a Business's BusinessItems
      
### Paging

Only a subset of results is returned, based on provided offsets.
      
### Results

The returned data for each Business includes its hours of operation
for the current day of the week. This is inconvenient because it makes
the query referentially non-transparent.  That is, the same query on
different days of the week may produce different results.  Such
behavior makes it difficult to cache the results.

Perhaps it would be better simply to return the hours of operation for
every day of the week and allow the client to decide how to use that
data.
      
## Endpoint

The path: `/v1/businesses`

It requires query-string parameters to work correctly.
