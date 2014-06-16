# Filtering: Business Hours

Allows one to specify a day (of the week) and time.  Only businesses
open then are returned.

For many businesses, we lack information about hours of operation.
For filtering purposes, those businesseses will always be considered
closed (and thus filtered out).

* name: `hours`
* optional - if absent, no filtering is performed
* type: formatted string
  * format: `day-of-week/hour:minute`
  * values:
    * day-of-week: integer in range `[0..6]`
    * hour: integer in range `[0..23]`
    * minute: integer in range `[0..59]`
    * e.g.: `0/19:30` means Sunday at 7:30pm
