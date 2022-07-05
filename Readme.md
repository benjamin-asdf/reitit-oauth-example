Do some steps here

https://developers.google.com/identity/protocols/oauth2

keep in mind this

https://github.com/weavejester/ring-oauth2

and this

https://github.com/metosin/reitit/issues/205


what I ran into:

1) first I wrapped session twice because I added `wrap-defaults`
middleware + session to the global middlewares

2) the global middlewares run first or something
either way I needed the parameter middleware in front of the oauth2.
And that was not the case when I had parameter in the router data.
