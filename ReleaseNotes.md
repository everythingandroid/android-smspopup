# Release Notes #
  * **v1.3.0** (Nov 14, 2013)
    * Add better support for Android 4.4 KitKat (see [FAQ](http://goo.gl/XYXzSC) for limitations)
    * Add better support for Hangouts as SMS app
    * Bug fixes
  * **v1.2.4** (May 6, 2012)
    * Quick fix for missed notifications when popup disabled
  * **v1.2.3** (May 5, 2012)
    * Quick fix for regular Reply force close
  * **v1.2.2** (May 5, 2012)
    * Fix quick reply failing to send on Samsung devices
    * Fix popup showing in messaging app on Samsung devices
    * Make popup size more dynamic (size changes from 1 line of text up to 6 now)
    * Fix popup time stamp issues on some US CDMA carrier devices (Verizon/Sprint)
    * Fix rogue messages appearing from "(unknown)"
    * Add timeout around acquiring Android partial wakelock to make sure device goes back to sleep
    * Add back "Subject" line to MMS messages
  * **v1.2.1** (April 20, 2012)
    * Bug fix release
  * **v1.2.0** (April 14, 2012)
    * Brand new holo theme for ICS devices
    * Browse multiple unread messages in popup by swiping left/right
    * Full re-write of much of the app to take advantage of newer Android versions
    * New option to show an unlock button when screen locked to prevent accidental button presses
    * Update to language translations
    * This version no longer supports API levels <7 (mostly cupcake/donut)
  * **v1.1.0** (December 17th, 2010)
    * Refreshed translations from launchpad
    * Added new option to toggle notification sound playing while on a call
    * Added a number of new icon choices to the notification icon option (also updated the ListView to show a preview of the icon)
    * Changed autolink text color to white (phone numbers, web links etc)
  * **v1.0.9** (August 17th, 2010)
    * Bug fix: Messages sent from quick reply on Droid X (or any Motoblur phone) were resending on reboot
    * Bug fix: Random crashes on Droid after Froyo update
    * Bug fix: Regular reply was not opening correct conversation in messaging app (it was opening last viewed conversation)
    * Enable choice of notification icon (3 choices now, will add more in next release)
    * Enable new privacy controls (hide sender + always use privacy)
    * Ignore Sprint visual voicemail messages
    * Refreshed translations from launchpad
  * **v1.0.8** (Feb 22nd, 2010)
    * Bug fix: notification customization for sms from email gateways working again
    * Updated translations, now using launchpad (thanks Macarse!)
    * Support for docked mode (prevent popup when docked)
    * Some MMS bug fixes
    * Quick Contact support for Android 2.0+
  * **v1.0.7** (Jan 24th, 2010)
    * Auto show soft keyboard on quick reply
    * New option to leave screen off on popup
    * Bug fixes
  * **v1.0.6** (Dec 10th, 2009)
    * Added signature option for Quick Reply
    * Bug fixes
  * **v1.0.5** (Nov 20th, 2009)
    * Now using Android TTS for devices on Donut and above
    * Bug fix: contact notification prefs not sticking when custom chosen (for led pattern, led color, vibrate pattern)
    * Bug fix: db cursor not being closed correctly
    * Blur background removed (causing lag on quick reply)
    * Change reply to use threadId again (option in button config to reply by phone number)
  * **v1.0.4** (Nov 12th, 2009)
    * Bug fix for voice transcription
    * Bug fix for slowness on Droid (blurring background causing problems on Android 2.0)
    * Bug fix for email->sms gateway messages
  * **v1.0.3** (Nov 12th, 2009)
    * Bug fix for TTS
  * **v1.0.2** (Nov 11th, 2009)
    * Added option to switch Quick Reply on/off
    * Added better support for Android 2.0, Eclair (helps for the recently released Droid)
    * Various bug fixes
  * **v1.0.1** (Nov 1st, 2009)
    * Switched back to regular reply as default
    * Updates to quick reply layout (made send button more easily accessible)
    * Turned off notifications while on phone (will make this configurable in a future update)
    * Fixed contact photo showing on HTC Hero
    * Fixed reply intent so it works correctly with Google Voice
  * **v1.0.0** (Oct 28th, 2009)
    * Quick reply directly from poup
    * Reply by voice using quick reply and voice transcription
    * Store preset messages and reply using them directly from popup
    * Customize notifications, popup, vibrate, LED on a per-contact basis (this can be used to effectively ignore certain users or single out others)
    * Configure the buttons on the popup
    * Use just the notifications, just the popup or both
    * Bug fixes and minor enhancements galore (better clearing of notification icon, alerts while on the phone, contact photo image resizing for larger contact photos)
    * Compatible with devices that run Android 1.5 (Cupcake) and above - but still includes support for multiple resolutions (both smaller like QVGA and larger like WVGA)
  * **v0.9.97** (June 21st, 2009)
    * Fixed bug in Donut OS version (config activity would crash)
    * Switched to Cupcake (1.5) SDK, users on prior OS versions will no longer see this app in the Market nor will this APK work on their devices (you can use 0.9.96 but I would suggest you upgrade to Cupcake)
    * Added Chinese (simplified, China) and Chinese (traditional, Taiwan) localizations
    * Added French, Russian, Spanish localizations to main package
  * **v0.9.96** (May 11th, 2009)
    * Fixed a minor bug where the view button (for privacy mode) was not showing correctly (again, d'oh)
  * **v0.9.95** (May 9th, 2009)
    * Fixed a minor bug where the view button (for privacy mode) was showing by accident
  * **v0.9.94** (May 9th, 2009)
    * Minor Cupcake fixes: privacy mode works correctly, soft keyboard shows for custom vibrate
    * Popup no longer shows when in system messaging app
    * New option to hide all buttons in the long-press (context) menu
  * **v0.9.93** (April 9th, 2009)
    * Bug fix for notifications sometimes not triggering if phone sleeps with conversation open in messaging app
  * **v0.9.92** (April 2nd, 2009)
    * Improvements for status bar notifications (removed correctly now in certain circumstances)
    * Threads marked read correctly now when a message is deleted via the popup
    * Cupcake compatibility
    * Beta feature: Text-to-speech, hear your messages!  Long-press on the popup to try this out.
  * **V0.9.91** (March 1st, 2009)
    * Bug fix for notifications not running sometimes
  * **V0.9.9** (Feb 22nd, 2009)
    * Screen goes off immediately after popup timeout now
    * New option to "dim screen" when popup shows
    * New option to turn screen on again for reminders
    * New option to customize specific color for LED
    * New option to customize LED blink rate
    * Notifications now update rather than clear if there are still unread messages
    * Individual messages are now marked as read when clicking "Close" (rather than the whole thread)
    * Russian localization
  * **v0.9.8** (Feb 14th, 2009)
    * Bug fix for the Delete button
  * **v0.9.7** (Feb 14th, 2009)
    * Fixed crash (force close) when receiving MMS messages without a subject
    * Added an option to show a Delete button on the popup
    * Spanish localization
  * **v0.9.6** (Feb 8th, 2009)
    * Bug fixes, all longer running code now runs via a service rather than in a broadcast receiver
    * Created generalized message class and adapted all code to use it
    * Better MMS support, now treated similar to SMS with popup and reminders
    * German and Dutch localizations added (thanks to Feyyaz and Melle)
  * **v0.9.5** (Jan 31st, 2009)
    * Bug fixes, moved some broadcast receivers to run their tasks in a service now to prevent "application not responding" type messages)
    * Selecting "Reply" will now always clear the notification icon
    * Reorganized the WakeLock code to be a lot cleaner
  * **v0.9.4** (Jan 23rd, 2009)
    * Cleaned up WakeLock code to prevent some "force close" messages
    * Added basic MMS support (very buggy still), at the moment an incoming MMS just triggers a notification but no popup (and the screen will not turn on)
    * Configurable vibrate patterns - choose from a list of pre-set vibrate patterns or create your own :)
    * Fixed the landscape layout so it fits correctly when the contact has a photo
  * **v0.9.3** (Jan 11th, 2009)
    * Minor bug fix, Keyguard was not re-engaging in a specific scenario
  * **v0.9.2** (Jan 11th, 2009)
    * Bug fixes
    * "Mark Read": the single most requested feature, you can now choose to have the conversation thread marked as read when you click the "Close" button
    * "Restrict Popup": this will restrict the popup window into only showing when the screen is off and keyguard is on (a lot of people asked if I could suppress the popup when they are in the SMS app writing a message - unfortunately this is not possible with the current SDK so this option is the next best thing)
    * Built in Notifications: the app now has its own notification system - if you use this, you should disable your system message notifications (and any 3rd party notifications) or you may see conflicts (and multiple notifications). I tried to make the notifications as customizable as possible - you can choose a sound, vibrate, LED blinking (with a choice of color).
    * Notification Reminders: if you use the app notifications you can also choose to be reminded - the number of reminders and interval between reminders is configurable
  * **v0.9.1** (Dec 24th, 2008)
    * Bug fixes
    * Added placeholder for Notifications (not functioning yet though)
  * **v0.9.0 - Initial version** (Dec 22nd, 2008)
    * Show a popup dialog when a you receive an SMS
    * The screen will wake up for a user defined time
    * The secure keyguard will be disabled (only for the initial popup, after that you will need to unlock as normal)
    * Close or Reply to the message from the popup
    * If more than 1 message is pending an Inbox button is shown
    * The Reply button calls the proper system intent, if there are any other applications available that can provide this functionality a dialog should show offering a choice of which application to use to reply.