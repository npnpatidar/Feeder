{
  description = "Android project";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
    flake-utils.url = "github:numtide/flake-utils";
    android-nixpkgs.url = "github:tadfisher/android-nixpkgs";
  };

  outputs = { self, nixpkgs, flake-utils, android-nixpkgs }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs {
          inherit system;
          config.android_sdk.accept_license = true;
        };
        android-sdk = android-nixpkgs.sdk.${system} (sdkPkgs:
          with sdkPkgs; [
            cmdline-tools-latest
            platform-tools
            # Keep the old versions for compatibility
            # build-tools-34-0-0
            # platforms-android-34
            # Add the required new versions for AGP 8.11.1
            build-tools-35-0-0
            platforms-android-35
          ]);
      in {
        devShell = pkgs.mkShell {
          buildInputs = [ pkgs.jdk17 android-sdk ];

          ANDROID_SDK_ROOT = "${android-sdk}/libexec/android-sdk";
        };
      });
}

