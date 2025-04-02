# EarlyTags
Inspect tags and other data WAY before it should be available.

## FAQ:

**How early can you use EarlyTags?**

As soon as Fabric has constructed the ModContainers, which could be at mixin-time before Bootstrap.


**Wait, what?**

Please submit your questions in writing. For the quickest response times, please use Classical Latin.


**Tags aren't available that early!**

Your mom also "wasn't available," but that didn't stop us last night.


**How can you have tags before the world is loaded / the client is synced?**

A lot of reading, and a little bit of black magic. You know, measure twice, cut once.


**Is this an April Fools' joke?**

The best jokes are completely true. This is one of those.


**How do I use this?**

Include this mod as an "include" (the whole library is 16KiB, of which 4KiB is the icon) and call an accessor on `blue.endless.earlytags.EarlyTags`. The EarlyTag object you get can then do membership tests with `contains`. Everything is by Identifier since items and blocks probably won't exist yet.
