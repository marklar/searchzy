
== Sorting by Quality ==

We want to sort the BusinessMenuItems based on the quality of the
business/service.

We have two such quality-related sorts:
  * by "value"
  * by "rating"

In each case, we compute an "awesomeness" value for each
BusinessMenuItem, and then we sort by that "awesomeness".

Out means of computing "awesomeness" is very similar for both methods.
They differ only in how their constituent parts are weighted.

Let's first look at how we compute the constituent parts of the score,
and then how the final scores are computed.


= Yelp Data =

Both the "value" and the "rating" sort use Yelp data.

Yelp provides up to two signals per Business (and thus BMI):
  * the number of user reviews ("count"), and
  * the average value of those reviews ("star rating"),
    between 0.0 and 5.0.

Smoothing.  In some cases, a Business hasn't been rated.  But we need
each Business to have non-zero values for these signals, so we provide
a default:
  * count: 1
  * stars: 3.0

Tweaking.  Statistically speaking, the greater the number of reviews,
the more dependable their "star rating".  Also, a large number of
positive reviews indicates a broader level of support for the
Business, and a large number of negative reviews indicates a broad
level of frustration.  We want to capture these insights in our
"awesomeness" score.

To do this, first we add a "tweaked" version of the review count.  If
the star rating is generally negative (< 3.0), then we multiply it by
-1.  So the more reviews resulting in a negative star count a Business
has, the lower its "tweaked" review count.

Normalizing.  Then, with both the review count and the star rating, we
want to assign a normalized score -- between 0 and 1.

(Perhaps the most obvious way to do this would be a straight mapping
between the value and where it lies in the range of values.  For
example, if the lowest star rating were 2.0 and the highest 5.0, then
a Business with a star rating of 2.5 would be 1/6th of the way between
the lowest and highest, for a score of 0.167.  ...But we don't do
this.  Instead...)

We have chosen a slightly more complicated way to assign these
normalized scores.  We use the *rank* of the value within the range of
values.  In other words, even if a Business had a star rating close to
the maximum (5.0), if the majority had even higher ratings, it would
rank lowly and get a low score.  For example, if it ranked only 10th
out of 15 ratings, it would get a normalized score of ((15 - 10) / 15)
== 0.333.


= Price Data =

Both the "value" and the "rating" sort also use price data.

Smoothing.  Not all BusinessMenuItems have prices, but since we want
to score them (in part) based on price, we give those that lack a
price that of the highest possible.

Normalize.  Then, as with Yelp data, we want to give the BMI a
normalized score based on its price.  And as with Yelp data, we use
the price's rank within the range of prices to assign the score.


= Awesomeness: Combine Them =

Finally, we calculate the "awesomeness" score as a function of the
normalized Yelp and price scores.  The "value" version of
"awesomeness" and the "rating" version of same differ only in the
weights applied to the Yelp and price scores.

              "value"  "rating"
  price_score   0.5      0.1
  count_score   0.25     0.4
  stars_score   0.25     0.4

As you can see, the "value" version gives much more weight to the
price, whereas "rating" favors the Yelp scores.
