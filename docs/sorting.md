
# Sorting

## Business Menu Items

* name: `sort`
* type: enum `{price, -price, value, -value, distance, -distance}`
* optional: defaults to `-value`
* order:
  * A `-` prefix means DESC order.  (You'll probably want to use `-value`.)
  * No prefix means ASC order.  (That's probably what you'll want to use for `distance` and `price`.)


## Businesses

* name: `sort`
* type: enum `{value, -value, distance, -distance, score, -score}`
* optional: defaults to `-value`
* order:
  * A `-` prefix means DESC order.  (You'll probably want to use `-value` and `-score` ".)"
  * No prefix means ASC order.  (You'll probably want to use `distance`.)
