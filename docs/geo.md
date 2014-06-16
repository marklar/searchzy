# Filtering: Geographic Distance

## Distance (Radius)

* name: `miles`
* type: float
* optional: defaults to `4.0`
* purpose: Define the radius (in miles) for the proximity filter.
   
## Location

Filter results by proximity to this location.

`Required` : either `address` -or- both `lat` and `lon`.

* address
  * name `address`
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
