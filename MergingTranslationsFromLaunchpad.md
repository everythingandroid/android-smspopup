# Explanation of how to merge translation files from launchpad to the svn repo.

# Introduction #
This guide will teach you how to merge the translation done in launchpad to the svn repo.


# Details #

  * Get smspopup src code from http://code.google.com/p/android-smspopup/source/checkout
  * Get android2po from http://github.com/miracle2k/android2po
  * Follow the installation instructions for android2po. In my case was:

```
python setup.py install
```

  * Download po files from launchpad doing:

  1. Go to: https://translations.launchpad.net/smspopup
  1. Click in Download: https://translations.launchpad.net/smspopup/trunk/+export
  1. Request the download. (It will arrive in your mail with an url like: http://launchpadlibrarian.net/XXXX/launchpad-export.tar.gz)

  * Go to the SMSpopup folder
  * Create the locale folder with:
```
a2po init
```

  * Copy the content of export.tar.gz to /locale
  * Update the /res folder doing:
```
a2po import
```

# Issues #
If you are using a mac, you might need to call a2po like this:

```
LC_CTYPE="en_GB.utf-8" a2po /*something*/
```

Check http://babel.edgewall.org/ticket/200 for more info. (Thanks Michael Elsdörfer!)