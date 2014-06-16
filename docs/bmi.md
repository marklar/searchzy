# Business Menu Items

## Try It

First, try a couple of queries.  Be sure to have Searchzy running on
your local machine at port 3000 (the default).

In the results, be sure to have a look at the `arguments`, to get a
sense for how it's interpreting the query-string parameters.
      
Using `address` and specifying some optional values, `miles`, `from`,
and `size`:

* [/v1/business_menu_items ?
   item_id=5076ea696bddbbdcc000000e &
   sort=price &
   address=New%20York,%20NY &
   miles=2.5 &
   from=0 &
   size=8
  ](http://localhost:3000/v1/business_menu_items?item_id=5076ea696bddbbdcc000000e&sort=price&address=New%20York,%20NY&miles=2.5&from=0&size=8)
      
Using `lat` and `lon` instead of `address`:

* [/v1/business_menu_items ?
   item_id=5076ea696bddbbdcc000000e &
   sort=price &
   lat=40.714 &
   lon=-74.006
  ](http://localhost:3000/v1/business_menu_items?item_id=5076ea696bddbbdcc000000e&sort=price&lat=40.714&lon=-74.006)
      
## Behavior
      
### Query

The user selects a BusinessCategory from an auto-suggest list.  Then
either the user or the application selects one Item from that
BusinessCategory.  The application then queries for local Businesses
which contain a BusinessUnifiedMenuItem of that Item.
      
For example, the user selects the BusinessCategory "Hair Salons", and
the application selects the default Item "Men's Haircut".  The app
queries Searchzy, which returns information about local Businesses
that provide "Men's Haircut".
      
### Results

Each result in the search results could be thought of as a "Business +
MenuItem".  It combines information about the Business provider
(e.g. its name, location, hours of operation, etc.)  and the MenuItem
itself (i.e. its price).
      
Searchzy also provides aggregate information about those "Business +
MenuItem" combos, including until what time the last relevant Business
is open, and what the min, max, and mean prices are for all MenuItems.
      
### Filtering

Of Businesses, by:

* Geographical proximity to a geo-point
* Optionally: Time and date - whether open
      
### Sorting

Various options. By any of:

* Price
* Value
* Distance
      
### Paging

Only a subset of results is returned, based on provided offsets.
      
## Endpoint

The path: `/v1/business_menu_items`

It requires query-string parameters to work correctly.
      
## Query String Parameters

### API Key

If you choose to add an `api-key` to your .config.yaml, then you'll
need to pass the same value in your `api_key` query-string parameter
with every request.

Please note: even though the config file uses a dash, this (and all
query-string parameters) uses an UNDERBAR.

* name: `api_key`
* type: string
* optional - needed only when the server requires an API key

### Query

* name: `query`
* type: string
* required
* purpose: for searching against names of Businesses
* interpretation:
  * expected to be complete words (not just prefixes)
  * treated as case-insensitive

### Sorting

* name: `sort`
* type: enum `{price, -price, value, -value, distance, -distance}`
* optional: defaults to `-value`
* order:
  * A `-` prefix means DESC order.  (You'll probably want to use `-value`.)
  * No prefix means ASC order.  (That's probably what you'll want to use for `distance` and `price`.)

### Paging

* start index
  * name: `from`
  * type: integer
  * optional: defaults to `0`
  * notes:
    * Indices start at `0`.
    * If there are fewer than `from` results, return `[]`.
* number to include
  * name: `size`
  * type: integer
  * optional: defaults to `10`
  * note: If there are fewer than `size` available, just return those.

### Filtering: Business Hours

Allows one to specify a day (of the week) and time.  Only businesses
open then are returned.  For many businesses, we lack information
about hours of operation.  For filtering purposes, those businesseses
will always be considered closed (and thus filtered out).

* name: `hours`
* optional - if absent, no filtering is performed
* type: formatted string
  * format: `day-of-week/hour:minute`
  * values:
    * day-of-week: integer in range `[0..6]`
    * hour: integer in range `[0..23]`
    * minute: integer in range `[0..59]`
    * e.g.: `0/19:30` means: Sunday at 7:30pm

### Filtering: Geographic Distance

#### Distance (Radius)

* name: `miles`
* type: float
* optional: defaults to `4.0`
* purpose: Define the radius (in miles) for the proximity filter.
   
#### Location

Filter results by proximity to this location.

`Required`: either `address` -or- both `lat` and `lon`.

* address
  * name: `address`
  * type: string
  * the value can take many different forms, such as:
    * just a zip: `94303`
    * city, state: `New York, NY`
    * a full street address: `123 Main St., New York, NY`
  * notes:
    * Input to a geo-coding service (Google or Bing).
    * To be used only if either `lat` or `lon` is absent.

* latitude AND longitude
  * latitude
    * name: `lat`
    * type: float
    * e.g. `10.347372387`
  * longitude
    * name: `lon`
    * type: float
    * e.g. `40.278171872`
