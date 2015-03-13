<h1>SMS Popup - Frequently Asked Questions</h1>




---

### Does SMS Popup work in Android 4.4 KitKat+? I heard there were some changes to the way SMS works in KitKat. ###
It does work, however there are some limitations. SMS Popup cannot delete messages nor can it mark messages as read in Android 4.4 KitKat and above. Otherwise SMS Popup still works fine though and can work alongside the chosen default SMS app. It can still send display messages, quick reply to messages directly (quick reply) and start the default SMS app to reply to messages.

### Does SMS Popup work with the Google Hangouts app set as the default SMS app? ###
Yes it does (but only for SMS of course). However the limitations mentioned in the above question still apply. Also in versions of Android before KitKat the "inbox" intent (that is triggered when you choose inbox or from the notifications tray when you have multiple unread messages) will not take you to the Hangouts app. This is a limitation I have not found a workaround for yet. In Android 4.4 KitKat and above the Hangouts app (and any other default SMS app) should be triggered correctly in all cases.

### The contact photo in the popup doesn't use the user's facebook photo, even though it shows in the system contact manager, why? ###
Unfortunately the official Facebook Android application doesn't allow third part apps to access the photos it pulls down.  As a workaround, use an app like [SyncMyPix](http://www.appbrain.com/app/com.nloko.android.syncmypix) to synchronize your facebook contact photos to your gmail (and therefore main phone) contacts list.

### I like/don't like the new Quick Reply feature introduced in v1, can I turn it back on/off? ###
~~You can revert back to the old Reply functionality very easily: Go into SMS Popup -> Additional Settings -> Button Configuration -> Button 3 Configuration -> Change this to "Reply" (instead of "Quick Reply").  This button will now start your system messaging app again.~~

As of v1.0.2 there is a quick switch option for Quick Reply under Additional Options->Quick Reply.

### The notification icon doesn't seem to go away when I use Quick Reply, what's going on? ###
This is likely because you have the system messaging app notifications still enabled or another apps notifications enabled for incoming messages.  Unfortunately there is no way for SMS Popup to remove another apps notifications at the moment.  Instead you will either have to:
  * [disable system notifications](#I_have_enabled_SMS_Popup's_notifications,_how_do_I_disable.md) and enable SMS Popup's notifications
  * **OR** you can [turn off Quick Reply](#I_like/don't_like_the_new_Quick_Reply_feature_introduced_in.md) as regular Reply allows the messaging app to remove the notifications.

### The notification icon doesn't seem to go away when I delete a message from the popup or close the popup, what's going on? ###
Same deal as [here](#The_notification_icon_doesn't_seem_to_go_away_when_I_use_Qu.md) - you will need to [disable system notifications](#I_have_enabled_SMS_Popup's_notifications,_how_do_I_disable.md) and use SMS Popup's notifications in order for the notification icon to be cleared.

### When will you add integration with Google Voice? ###
I would love to add better Google Voice integration, however at the moment (as at 2010-09-30) the Google Voice Android application offers only minimal system integration.  To take advantage of this, turn GV SMS notifications ON and then in SMS Popup settings -> Additional Settings -> Button Configuration, set one of the buttons to "Reply (to phone number)".  This will allow the Reply button to open the GV app to reply.  Of course this is far from ideal as the number on the popup will not show as the real phone number.

Once the GV app adds better integration I will do my best to support it.

### Help! Vibrate in SMS Popup notifications does not seem to work anymore. ###
I'm not sure why this happens, but there are reports that an app called [Sound Manager](http://www.appbrain.com/app/com.roozen.SoundManager) can both be the cause of this and the fix for this.

### Can I ignore messages from certain contacts?  Or can I only be notified about a handful of contacts and ignore the others? ###
Yes!  Starting from v1 you can configure custom per-contact notification settings.  Here are a few examples of how you can use this:

#1. Ignore just a handful of contacts
  * Set default notifications to ON
  * Create custom notifications for the users you want to ignore and turn off popups and notifications

#2. Notify for only a handful of contacts, ignore everyone else
  * Set default notifications to OFF
  * Create custom notifications for the users you would like to be notified on, setting popup or notifications to on

#3. Only show the popup for a few contacts and use regular notifications for everyone else
  * Set default notifications to ON but turn popups OFF
  * Create custom notifications for the users you want the popup to show for, setting popup and notifications on

### Help! Sound and vibration are not working when a message is received, what can I do? ###
First of all, ensure you have notifications enabled in SMS Popup and that your phone volume is on :)

People have been reporting that sound and vibration can sometimes stop randomly (but the LED keeps working).  I'm still trying to work out what is causing this so stay tuned for a proper fix.  You can verify this is your issue by running the "Test Notification" (sound/vibrate will not work there either).

To get things working again try:
> -making or receiving a call (I hear just dialing a number and hanging up can help)
> -rebooting your phone

### How can I use sound files from my SD card? ###
Install the Android Market application [Rings Extended](https://market.android.com/details?id=com.angryredplanet.android.rings_extended) - when you select the notification ringtone it will then allow you to choose from a variety of ringtone sources.

### I see/hear 2 (or more) notifications when I receive an SMS/MMS, what gives? ###
Ensure you have notifications disabled in other messaging apps, the most common will be the built in system messaging app ([instructions for turning off the system messaging app notifications](#I_have_enabled_SMS_Popup's_notifications,_how_do_I_disable.md)).

Other popular apps which trigger notifications include ChompSMS, Handcent SMS and other "missed alerts" type programs.

### Customizing the color of the LED doesn't seem to work properly, what's up with that? ###
The Android OS (and SDK) allows you to set the LED color to anything that you want - after which the hardware will **do its best** to show that color.  If the color you chose doesn't show properly then it is likely a hardware limitation.  Some newer Android phones may not even have an LED or may not have an LED that supports multiple colors.

The one color which has me stumped is why choosing white will show a purple LED (at least on the G1).  But either way, this is not a bug with SMS Popup but a result of the way the Android SDK works.

### Can you prevent the popup when I am inside the messaging app typing a message?  It's distracting! ###
As of v0.9.94 this should now be fixed.  I found a (hacky) way to detect if the user was previously in the message application.  Now the notification will show but the popup will not if you are actively in the messaging app.

### Can you make SMS Popup compatible with `<`insert 3rd party messaging app here`>`? (eg. ChompSMS or Handcent SMS) ###
Yes, it should already work with any 3rd party messaging app that provides the correct intents such as Chomp or Handcent (although from my tests these apps only offer the "Reply" type Intent and not the "Inbox" or "Conversation List" type Intent).

If this is not working for you, try clearing your app defaults for the built in messaging app:
  * System settings -> Applications -> Manage applications -> Messaging -> click "Clear defaults"

### I have enabled SMS Popup's notifications, how do I disable the system messaging app notifications? ###
Go to the messaging app -> hit menu -> settings -> scroll down and un-check "notifications"

### Can you explain how the custom vibrate pattern works? ###
It's really quite simple:
  * enter a string of numbers separated by commas
  * each number represents milliseconds
  * the first number in the list is time NOT to vibrate, the second number in the list is how long to vibrate - repeat this pattern for multiple vibrate patterns.

Examples:
  * A single long vibrate (don't wait, then vibrate for 3seconds): 0,3000
  * A bunch of short vibrations (don't wait, vibrate for 0.5seconds, pause for 0.2seconds, repeat): 0,500,200,500,200,500,200,500,200

### Known issues with HTC Hero (and possibly other devices running HTC's custom Sense UI) ###
| Issue | Resolution / Comments |
|:------|:----------------------|
| Contact photo not showing correctly on popup | This should be fixed in SMS Popup v1.0.1 |
| Messaging app icon on home screen shows wrong unread count | Unfortunately HTC did something custom for this icon and does not yet offer a way for 3rd party apps to refresh the icon, at this time there is no fix, however, a good option is to install the "[SMS Unread Count](https://market.android.com/details?id=com.kanokgems.smswidget)" widget from Market which does work correctly with SMS Popup and is basically the same |

### Known issues with the Samsung Moment ###
| Issue | Resolution / Comments |
|:------|:----------------------|
| Screen does not turn on when a message is received, later when the device is manually woken up all the popups show along with the notifications | Try turning off "Dim Popup Screen" (under Additional Settings), it seems the Moment doesn't correctly support the dim screen functions from the SDK |

### Known issues with the Motorola Droid ###
| Issue | Resolution / Comments |
|:------|:----------------------|
| Contacts from address book do not correctly show name and photo on popup | ~~This is due to the new multi-source address book in Android 2.0 (so any device with 2.0 should have this problem).  At the moment it will only match against contacts from the primary account on your phone.  I am working on updates to support all contacts and contact photos. So stay tuned.~~ This should be fixed now, if you have further issues, please add a new issue to the tracker. |

### Known issues with Sony Ericcson Android phones (like the X10 and X8 series) ###
| Issue | Resolution / Comments |
|:------|:----------------------|
| LED customization does not work after system update | It should still work, it may only show when the screen is off though so when testing the notification try turning the screen off to see if it works |
| When clicking "Reply" a blank message opens up without the recipient filled in | Try switching the Reply button type in SMS Popup Settings -> Additional Settings -> Button Configuration -> choose a button -> "Reply (to phone number)" |

### Other issues ###
| Issue | Resolution / Comments |
|:------|:----------------------|
| Messages appear to be gone in the messaging app after closing the popup | This is likely due to the system messaging app running out of memory and not being able to store the message in the system database.  This seems to occur far more often on custom roms like the Cyanogen Mod - especially when you have the "Lock Home in Memory" option on (this  blocks of a chunk of system memory). |