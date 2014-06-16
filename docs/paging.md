# Paging

* start index
  * name: `from`
  * type: integer
  * optional: defaults to `0`
  * notes:
    * Indices start at `0".
    * If there are fewer than `from` results, return `[]`.
* number to include
  * name: `size`
  * type: integer
  * optional: defaults to `10`
  * note: If there are fewer than `size` available, just return those.
