# Welcome to PortSIP UC SDK For Android

Create your SIP-based application for multiple platforms (iOS, Android, Windows, Mac OS and Linux) with our SDK.

The rewarding PortSIP UC SDK is a powerful and versatile set of tools that dramatically accelerate SIP application development. It includes a suite of stacks, SDKs, and some Sample projects, with each of them enables developers to combine all the necessary components to create an ideal development environment for every application's specific needs.

The PortSIP UC SDK complies with IETF and 3GPP standards, and is IMS-compliant (3GPP/3GPP2, TISPAN and PacketCable 2.0).
These high performance SDKs provide unified API layers for full user control and flexibility.


## Getting Started

You can download PortSIP UC SDK Sample projects at our [Website](https://www.portsip.com/download-portsip-voip-sdk/).
 Samples include demos for VC++, C#, VB.NET, Delphi XE, XCode (for iOS and Mac OS), Eclipse (Java for Android) with the sample project source code provided (with SDK source code exclusive). The sample projects demonstrate how to create a powerful SIP application with our SDK easily and quickly.

## Contents

 The sample package for downloading contains almost all of materials for PortSIP SDK: documentation,
 Dynamic/Static libraries, sources, headers, datasheet, and everything else a SDK user might
 need!


## SDK User Manual

 To be started with, it is recommended to read the documentation of PortSIP UC SDK, [SDK User Manual page](https://www.portsip.com/voip-sdk-user-manual/), which gives a brief
 description of each API function.


## Website

Some general interest or often changing PortSIP SDK information will be posted on the [PortSIP website](https://www.portsip.com) in real time. The release contains links to the site, so while browsing you may see occasional broken links  if you are not connected to the Internet. To be sure everything needed for using the PortSIP UC SDK has been contained within the release.

## Support

Please send email to our <a href="mailto:support@portsip.com">Support team</a> if you need any help.

## Installation Prerequisites

To use PortSIP VoIP/IMS SDK for Android for development, SDK version with later than API-16 is required.

## Frequently Asked Questions
### 1. Does PortSIP UC SDK is free?

  Yes, the PortSIP UC SDK is totally free, but it was limited only works with <a href="https://www.portsip.com/portsip-pbx/" target="_blank">PortSIP PBX</a>.

### 2. What is the difference between PortSIP UC SDK and PortSIP VoIP SDK?
  The <a href="https://www.portsip.com/portsip-uc-sdk/" target="_blank">PortSIP UC SDK</a> is free, but was limited to works with <a href="https://www.portsip.com/portsip-pbx/" target="_blank">PortSIP PBX</a>; The <a href="https://www.portsip.com/portsip-pbx/" target="_blank">PortSIP VoIP SDK</a> is not free that can works with any 3rd SIP based PBX. The UC SDK also have a lot of unique features than the VoIP SDK which provided by <a href="https://www.portsip.com/portsip-pbx/" target="_blank">PortSIP PBX</a>.

### 3. Where can I download the PortSIP UC SDK for test?
  All sample projects of the %PortSIP UC SDK can be found and downloaded at:
  <a href="https://www.portsip.com/download-portsip-uc-sdk/" target="_blank">https://www.portsip.com/download-portsip-uc-sdk/</a> <br />

### 4. How can I compile the sample project?

  1. Download the sample project from PortSIP website.
  2. Extract the .zip file.
  3. Open the project by your Eclipse or Android studio:
  4. Compile the sample project directly.  


### 5. How can I create a new project with PortSIP VoIP SDK?
  1. Download the sample project and evaluation SDK and extract it to a specified directory
  2. Run Android Studio and create a new Android Application Project
  3. Copy all files form libs directory under extracted directory to the libs directory of your new application.
  4. Import the dependent class form the SDK. For example:
			import com.portsip.OnPortSIPEvent;
			import com.portsip.PortSipSdk;
  5. Inherit the interface OnPortSIPEvent to process the callback events.
  6. Initialize SDK. For example:
			mPortSIPSDK = new PortSipSdk();
			mPortSIPSDK.setOnPortSIPEvent(instanceofOnPortSIPEvent);
			mPortSIPSDK.CreateCallManager(context);
			mPortSIPSDK.initialize(...);
  For more details please refer to the Sample project source code.


### 6. Is the SDK thread safe?
    Yes, the SDK is thread safe. You can call any of the API functions without the need to consider the multiple threads.
Note: the SDK allows to call API functions in callback events directly - except for the "onAudioRawCallback", "onVideoRawCallback", "onReceivedRtpPacket", "onSendingRtpPacket" callbacks.
