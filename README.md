# SW Call Recorder

SW Call Recorder is an Android application that automatically records phone calls based on user selected phone numbers.
It has the following main features:

- The phone numbers are organized into contacts.
- Every contact can have an associated picture selected by the user.
- There are 4 possible recording formats: WAV (Lossless),  AAC 128kbs, AAC 64kbps and AAC 32kbps.
- Recordings can be mono or stereo.
- The device can be put automatically on speaker while the application records the call.
- Recordings can be stored in the application's private storage space or in a public accessible location on the device. If they are stored in a public accessible location, they can be easily transferred to other devices, like a laptop. The location of the recordings on the device can be easily changed.
- Phone calls from private (unknown) numbers are supported.
- The user can rename the recordings.
- The recordings can be played from within the application with the help of a dedicated audio player.
- The audio player can modify the sound volume of the device while playing. After the playing finishes, the audio volume of the device automatically comes back to the previous value.
- The audio player can apply a gain to the audio signal to further control how loud the sound is played.
- The application has 2 themes: light and dark.

Because of the restrictions imposed by Google on requesting permissions from the Call log permission group (https://play.google.com/about/privacy-security-deception/permissions/), the app is currently developed using two branches: the `original-app` branch and the `gpcompliant` (from "google play compliant") branch.

The `gpcompliant` branch does not use the READ_CALL_LOG and PROCESS_OUTGOING_CALLS permissions and consequently the versions of the app compiled from this source cannot obtain the outgoing phone number in all supported versions of Android and cannot obtain the incoming phone number in Android versions 9 and above. This has consequences on the overall appearance and behavior of the app that are documented in the "Recording phone calls" section of the Help menu. These versions have the substring 'gpcompliant' appended to the version string.

The `original-app` branch retains all the functions that were removed from the `gpcompliant` branch. The versions of the app compiled from this source have the substring 'original' appended to the version string.

<!--<a href="https://synaptic-call-recorder.en.aptoide.com/"><img width="135px" alt="Android app on Aptoide" src="https://cdn6.aptoide.com/includes/themes/2014/images/aptoide_badge.svg?timestamp=timestamp=20190529"></a>-->

<!-- <a href='https://play.google.com/store/apps/details?id=net.synapticweb.callrecorder.gpcompliant.full&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1'><img width="135px" alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'/></a> -->
