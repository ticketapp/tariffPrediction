#!/usr/bin/env python
import twitter
import json

#Setting up Twitter API
api = twitter.Api(
 consumer_key='wU7QIC8Sdnv0ne0BX9iUWUuOX',
 consumer_secret='kf7dKSPF3wLdgWLcbLPrGnawRvfpCN2CJY9TTCqsrt4Gf5ZATf',
 access_token_key='3229481342-XIhBf8wAGryMxQgQxOp4D3pGtvmtKYBGB7HTESG',
 access_token_secret='t6j9oOZeFZ5WaA3XvFKzvRVizE60xfdEwOusZPElT9okc'
 )
 


results = api.GetSearch(
    raw_query="screen_name=andypiper")