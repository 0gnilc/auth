# Keep dynamic internationalization auxiliary to business resources

Dynamic messages provide optional display text but do not validate, own, block, modify, or cascade into resources that reference their Message Keys. An embedded message editor saves explicitly and independently of its enclosing form, so either save may succeed without creating a transaction across the two resources.
