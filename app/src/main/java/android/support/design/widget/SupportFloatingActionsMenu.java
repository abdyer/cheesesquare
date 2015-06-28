package android.support.design.widget;

import android.content.Context;
import android.graphics.Rect;
import android.os.Build;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;

import com.support.android.designlibdemo.R;

import java.util.List;

/**
 * A {@link com.getbase.floatingactionbutton.FloatingActionsMenu} subclass that works with the design support library's {@link CoordinatorLayout}
 */
@CoordinatorLayout.DefaultBehavior(SupportFloatingActionsMenu.Behavior.class)
public class SupportFloatingActionsMenu extends com.getbase.floatingactionbutton.FloatingActionsMenu {

    public SupportFloatingActionsMenu(Context context) {
        super(context);
    }

    public SupportFloatingActionsMenu(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SupportFloatingActionsMenu(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // measured height includes child views, so translate Y to align FAB on view edges
        if (getTranslationY() == 0) {
            View fab = getChildAt(getChildCount() - 1); // last child is the FAB
            int shadowRadius = getResources().getDimensionPixelSize(R.dimen.fab_shadow_radius);
            int offset = (getMeasuredHeight() - fab.getMeasuredHeight() + shadowRadius) / 2;
            ViewCompat.setTranslationY(this, offset);
        }
    }

    public static class Behavior extends android.support.design.widget.CoordinatorLayout.Behavior<SupportFloatingActionsMenu> {
        private static final boolean SNACKBAR_BEHAVIOR_ENABLED;
        private Rect mTmpRect;
        private boolean mIsAnimatingOut;
        private float mTranslationY;

        public Behavior() {
        }

        public boolean layoutDependsOn(CoordinatorLayout parent, SupportFloatingActionsMenu child, View dependency) {
            return SNACKBAR_BEHAVIOR_ENABLED && dependency instanceof Snackbar.SnackbarLayout;
        }

        public boolean onDependentViewChanged(CoordinatorLayout parent, SupportFloatingActionsMenu child, View dependency) {
            if(dependency instanceof Snackbar.SnackbarLayout) {
                this.updateFabTranslationForSnackbar(parent, child, dependency);
            } else if(dependency instanceof AppBarLayout) {
                AppBarLayout appBarLayout = (AppBarLayout)dependency;
                if(this.mTmpRect == null) {
                    this.mTmpRect = new Rect();
                }

                Rect rect = this.mTmpRect;
                ViewGroupUtils.getDescendantRect(parent, dependency, rect);
                if(rect.bottom <= appBarLayout.getMinimumHeightForVisibleOverlappingContent()) {
                    if(!this.mIsAnimatingOut && child.getVisibility() == View.VISIBLE) {
                        this.animateOut(child);
                    }
                } else if(child.getVisibility() != View.VISIBLE) {
                    this.animateIn(child);
                }
            }

            return false;
        }

        private void updateFabTranslationForSnackbar(CoordinatorLayout parent, SupportFloatingActionsMenu fab, View snackbar) {
            float translationY = this.getFabTranslationYForSnackbar(parent, fab);
            if(translationY != this.mTranslationY) {
                ViewCompat.animate(fab).cancel();
                if(Math.abs(translationY - this.mTranslationY) == (float)snackbar.getHeight()) {
                    ViewCompat.animate(fab).translationY(translationY).setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR).setListener((ViewPropertyAnimatorListener)null);
                } else {
                    ViewCompat.setTranslationY(fab, translationY);
                }

                this.mTranslationY = translationY;
            }

        }

        private float getFabTranslationYForSnackbar(CoordinatorLayout parent, SupportFloatingActionsMenu fab) {
            float minOffset = 0.0F;
            List dependencies = parent.getDependencies(fab);
            int i = 0;

            for(int z = dependencies.size(); i < z; ++i) {
                View view = (View)dependencies.get(i);
                if(view instanceof Snackbar.SnackbarLayout && parent.doViewsOverlap(fab, view)) {
                    minOffset = Math.min(minOffset, ViewCompat.getTranslationY(view) - (float)view.getHeight());
                }
            }

            return minOffset;
        }

        private void animateIn(SupportFloatingActionsMenu button) {
            button.setVisibility(View.VISIBLE);
            if(Build.VERSION.SDK_INT >= 14) {
                // removed the scale X & Y to avoid strange animation behavior with the FAB menu
                ViewCompat.animate(button).alpha(1.0F).setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR).withLayer().setListener((ViewPropertyAnimatorListener)null).start();
            } else {
                Animation anim = android.view.animation.AnimationUtils.loadAnimation(button.getContext(), android.support.design.R.anim.fab_in);
                anim.setDuration(200L);
                anim.setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR);
                button.startAnimation(anim);
            }

        }

        private void animateOut(final SupportFloatingActionsMenu button) {
            if(Build.VERSION.SDK_INT >= 14) {
                // removed the scale X & Y to avoid strange animation behavior with the FAB menu
                ViewCompat.animate(button).alpha(0.0F).setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR).withLayer().setListener(new ViewPropertyAnimatorListener() {
                    public void onAnimationStart(View view) {
                        Behavior.this.mIsAnimatingOut = true;
                    }

                    public void onAnimationCancel(View view) {
                        Behavior.this.mIsAnimatingOut = false;
                    }

                    public void onAnimationEnd(View view) {
                        Behavior.this.mIsAnimatingOut = false;
                        view.setVisibility(View.GONE);
                    }
                }).start();
            } else {
                Animation anim = android.view.animation.AnimationUtils.loadAnimation(button.getContext(), android.support.design.R.anim.fab_out);
                anim.setInterpolator(AnimationUtils.FAST_OUT_SLOW_IN_INTERPOLATOR);
                anim.setDuration(200L);
                anim.setAnimationListener(new AnimationUtils.AnimationListenerAdapter() {
                    public void onAnimationStart(Animation animation) {
                        Behavior.this.mIsAnimatingOut = true;
                    }

                    public void onAnimationEnd(Animation animation) {
                        Behavior.this.mIsAnimatingOut = false;
                        button.setVisibility(View.GONE);
                    }
                });
                button.startAnimation(anim);
            }

        }

        static {
            SNACKBAR_BEHAVIOR_ENABLED = Build.VERSION.SDK_INT >= 11;
        }
    }
}
