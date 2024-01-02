#pragma once

@interface UnityViewControllerBase (iOS)
- (BOOL)shouldAutorotate;

- (BOOL)prefersStatusBarHidden;
- (UIStatusBarStyle)preferredStatusBarStyle;

- (void)viewWillTransitionToSize:(CGSize)size withTransitionCoordinator:(id<UIViewControllerTransitionCoordinator>)coordinator;
@end

// for better handling of user-imposed screen orientation we will have specific ViewController implementations

// view controllers constrained to one orientation

@interface UnityPortraitOnlyViewController : UnityViewControllerBase
{
}
@end
@interface UnityPortraitUpsideDownOnlyViewController : UnityViewControllerBase
{
}
@end
@interface UnityLandscapeLeftOnlyViewController : UnityViewControllerBase
{
}
@end
@interface UnityLandscapeRightOnlyViewController : UnityViewControllerBase
{
}
@end

// this is default view controller implementation (autorotation enabled)

@interface UnityDefaultViewController : UnityViewControllerBase
{
}

// we have well defined points where we should update supported orientations:
// on init and inside [UnityAppController checkOrientationRequest]
//   note that the latter will recreate default view controller if supported orientations conflict with the current orientation
// this is done as opposed to [UnityDefaultViewController supportedInterfaceOrientations] poking unity for that
//   as this might happen in "random" places, out-of-sync with our handling of "orientation constraints were changed at unity side"
- (void)updateSupportedOrientations;
@end

NSUInteger EnabledAutorotationInterfaceOrientations();
