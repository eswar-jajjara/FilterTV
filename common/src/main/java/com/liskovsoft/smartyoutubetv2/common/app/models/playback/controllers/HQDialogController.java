package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import com.liskovsoft.sharedutils.helpers.MessageHelpers;
import com.liskovsoft.smartyoutubetv2.common.R;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.BasePlayerController;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionCategory;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.OptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.ui.UiOptionItem;
import com.liskovsoft.smartyoutubetv2.common.app.presenters.AppDialogPresenter;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.FormatItem;
import com.liskovsoft.smartyoutubetv2.common.utils.AppDialogUtil;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HQDialogController extends BasePlayerController {
    private static final String TAG = HQDialogController.class.getSimpleName();
    // NOTE: using map, because same item could be changed time to time
    private final Map<Integer, OptionCategory> mCategories = new LinkedHashMap<>();
    private final Map<Integer, OptionCategory> mCategoriesInt = new LinkedHashMap<>();
    private final Set<Runnable> mHideListeners = new HashSet<>();
    private AppDialogPresenter mAppDialogPresenter;

    @Override
    public void onInit() {
        mAppDialogPresenter = AppDialogPresenter.instance(getContext());
    }

    @Override
    public void onViewResumed() {
        //updateBackgroundPlayback();
    }

    @Override
    public void onButtonClicked(int buttonId, int buttonState) {
        if (buttonId == R.id.lb_control_high_quality) {
            onHighQualityClicked();
        }
    }

    private void onHighQualityClicked() {
        if (getPlayer() == null) return;
        fitVideoIntoDialog();

        mAppDialogPresenter.appendSingleButton(UiOptionItem.from(getContext().getString(R.string.filtertv_quality),
                formatSummary(getPlayer().getVideoFormat()), item -> showFormatDialog(true)));
        mAppDialogPresenter.appendSingleButton(UiOptionItem.from(getContext().getString(R.string.filtertv_captions),
                formatSummary(getPlayer().getSubtitleFormat()), item -> showCaptionsDialog()));
        mAppDialogPresenter.appendSingleButton(UiOptionItem.from(getContext().getString(R.string.filtertv_audio),
                formatSummary(getPlayer().getAudioFormat()), item -> showFormatDialog(false)));
        mAppDialogPresenter.appendSingleButton(UiOptionItem.from(getContext().getString(R.string.filtertv_speed),
                getPlayer().getSpeed() + "×", item -> {
            mAppDialogPresenter.appendCategory(AppDialogUtil.createSpeedListCategory(getContext(), getPlayer()));
            mAppDialogPresenter.showDialog();
        }));
        mAppDialogPresenter.appendSingleButton(UiOptionItem.from(getContext().getString(R.string.filtertv_repeat), item -> {
            OptionCategory category = AppDialogUtil.createPlaybackModeCategory(getContext(),
                    () -> getPlayer().setButtonState(R.id.action_repeat, getPlayerData().getPlaybackMode()));
            mAppDialogPresenter.appendRadioCategory(category.title, category.options);
            mAppDialogPresenter.showDialog();
        }));
        mAppDialogPresenter.appendSingleButton(UiOptionItem.from(getContext().getString(R.string.playback_settings), item -> showAdvancedSettings()));

        mAppDialogPresenter.showDialog(getContext().getString(R.string.filtertv_player_settings), this::onDialogHide);
    }

    private CharSequence formatSummary(FormatItem format) {
        if (format == null) return "";
        if (format.isDefault()) return getContext().getString(R.string.option_disabled);
        if (format.getType() == FormatItem.TYPE_VIDEO && format.getHeight() > 0) return resolution(format) + "p";
        if (format.getType() == FormatItem.TYPE_AUDIO) {
            String language = format.getLanguage();
            if (language == null || language.isEmpty()) return getContext().getString(R.string.filtertv_original_audio);
            return language;
        }
        return format.getTitle();
    }

    private static int resolution(FormatItem format) {
        return format.getWidth() > 0 ? Math.min(format.getWidth(), format.getHeight()) : format.getHeight();
    }

    private void showFormatDialog(boolean video) {
        if (getPlayer() == null) {
            return;
        }
        List<FormatItem> formats = video ? getPlayer().getVideoFormats() : getPlayer().getAudioFormats();
        String title = getContext().getString(video ? R.string.filtertv_quality : R.string.filtertv_audio);
        if (video) {
            java.util.Map<Integer, FormatItem> resolutions = new java.util.LinkedHashMap<>();
            if (formats != null) for (FormatItem format : formats) {
                if (format.isDefault() || format.getHeight() <= 0) continue;
                FormatItem previous = resolutions.get(resolution(format));
                if (previous == null || format.isSelected()) resolutions.put(resolution(format), format);
            }
            List<OptionItem> options = new java.util.ArrayList<>();
            for (FormatItem format : resolutions.values()) {
                OptionItem detailed = UiOptionItem.from(format, this::selectFormatOption);
                options.add(UiOptionItem.from(resolution(format) + "p", ignored -> selectFormatOption(detailed), format.isSelected()));
            }
            mAppDialogPresenter.appendRadioCategory(title, options);
        } else {
            List<OptionItem> options = new java.util.ArrayList<>();
            options.add(UiOptionItem.from(getContext().getString(R.string.filtertv_original_audio), ignored -> {
                getPlayerData().setAudioLanguage("");
                getPlayer().setFormat(getPlayerData().getFormat(FormatItem.TYPE_AUDIO));
            }, getPlayerData().getAudioLanguage().isEmpty()));
            java.util.Set<String> languages = new java.util.LinkedHashSet<>();
            if (formats != null) for (FormatItem format : formats) {
                String language = format.getLanguage();
                if (format.isDefault() || language == null || language.contains("original") || !languages.add(language)) continue;
                OptionItem detailed = UiOptionItem.from(format, this::selectFormatOption);
                options.add(UiOptionItem.from(language, ignored -> {
                    getPlayerData().setAudioLanguage(language.split(" ")[0]);
                    selectFormatOption(detailed);
                }, format.isSelected()));
            }
            mAppDialogPresenter.appendRadioCategory(title, options);
        }
        mAppDialogPresenter.showDialog();
    }

    private void showCaptionsDialog() {
        if (getPlayer() == null) {
            return;
        }
        mAppDialogPresenter.appendRadioCategory(getContext().getString(R.string.filtertv_captions),
                UiOptionItem.from(getPlayer().getSubtitleFormats(), option -> {
                    FormatItem format = UiOptionItem.toFormat(option);
                    getPlayer().setFormat(format);
                    getPlayerData().setFormat(format);
                }, getContext().getString(R.string.subtitles_disabled)));
        mAppDialogPresenter.showDialog();
    }

    private void showAdvancedSettings() {
        mCategoriesInt.clear();

        addAudioLanguage();
        mCategoriesInt.put(-1001, OptionCategory.radioList(getContext().getString(R.string.filtertv_quality),
                UiOptionItem.from(getPlayer().getVideoFormats(), this::selectFormatOption, getContext().getString(R.string.option_disabled))));
        mCategoriesInt.put(-1002, OptionCategory.radioList(getContext().getString(R.string.filtertv_audio),
                UiOptionItem.from(getPlayer().getAudioFormats(), this::selectFormatOption, getContext().getString(R.string.option_disabled))));
        addPresetsCategory();
        addVideoZoomCategory();
        addNetworkEngine();
        addVideoBufferCategory();
        addAudioDelayCategory();
        addPitchEffectCategory();
        addSleepTimerCategory();
        //addBackgroundPlaybackCategory();

        appendOptions(mCategoriesInt);
        appendOptions(mCategories);

        mAppDialogPresenter.showDialog();
    }

    private void selectFormatOption(OptionItem option) {
        if (getPlayer() == null) {
            return;
        }

        FormatItem formatItem = UiOptionItem.toFormat(option);
        getPlayer().setFormat(formatItem);
        persistFormat(formatItem);

        if (getPlayerData().getFormat(formatItem.getType()).isPreset()) {
            // Preset currently active. Show warning about format reset.
            MessageHelpers.showMessage(getContext(), R.string.video_preset_enabled);
        }

        if (!getPlayer().containsMedia()) {
            getPlayer().reloadPlayback();
        }

        // Make result easily be spotted by the user
        if (formatItem.getType() == FormatItem.TYPE_VIDEO) {
            getPlayer().showOverlay(false);
        }
    }

    private void persistFormat(FormatItem formatItem) {
        if (formatItem.getType() == FormatItem.TYPE_VIDEO) {
            if (!getPlayerData().getFormat(FormatItem.TYPE_VIDEO).isPreset()) {
                getPlayerData().setFormat(formatItem);
            } else {
                getPlayerData().setTempVideoFormat(formatItem);
            }
        } else {
            getPlayerData().setFormat(formatItem);
        }
    }

    private void addVideoBufferCategory() {
        if (getPlayer() == null) {
            return;
        }
        addCategoryInt(AppDialogUtil.createVideoBufferCategory(getContext(),
                () -> getPlayer().restartEngine()));
    }

    private void addAudioDelayCategory() {
        if (getPlayer() == null) {
            return;
        }
        addCategoryInt(AppDialogUtil.createAudioDelayCategory(getContext(),
                () -> getPlayer().restartEngine()));
    }

    private void addPitchEffectCategory() {
        addCategoryInt(AppDialogUtil.createPitchEffectCategory(getContext()));
    }

    private void addSleepTimerCategory() {
        addCategoryInt(AppDialogUtil.createSleepTimerCategory(getContext()));
    }

    private void addAudioLanguage() {
        if (getPlayer() == null) {
            return;
        }
        addCategoryInt(AppDialogUtil.createAudioLanguageCategory(getContext(),
                () -> getPlayer().restartEngine()));
    }

    private void addNetworkEngine() {
        if (getPlayer() == null) {
            return;
        }
        addCategoryInt(AppDialogUtil.createNetworkEngineCategory(getContext(),
                () -> getPlayer().restartEngine()));
    }

    private void onDialogHide() {
        //updateBackgroundPlayback();

        for (Runnable listener : mHideListeners) {
            listener.run();
        }

        mHideListeners.clear();
        mCategories.clear();
        mCategoriesInt.clear();
    }

    //private void updateBackgroundPlayback() {
    //    ViewManager.instance(getContext()).blockTop(null);
    //
    //    if (getPlayer() != null) {
    //        getPlayer().setBackgroundMode(getPlayerData().getBackgroundMode());
    //    }
    //}

    //private void addBackgroundPlaybackCategory() {
    //    OptionCategory category =
    //            AppDialogUtil.createBackgroundPlaybackCategory(getContext(), getPlayerData(), GeneralData.instance(getContext()), this::updateBackgroundPlayback);
    //
    //    addCategoryInt(category);
    //}

    private void addPresetsCategory() {
        addCategoryInt(AppDialogUtil.createVideoPresetsCategory(
                getContext(), () -> {
                    if (getPlayer() == null) {
                        return;
                    }

                    FormatItem format = getPlayerData().getFormat(FormatItem.TYPE_VIDEO);
                    getPlayer().setFormat(format);

                    if (!getPlayer().containsMedia()) {
                        getPlayer().reloadPlayback();
                    }

                    // Make result easily be spotted by the user
                    getPlayer().showOverlay(false);
                }
        ));
    }

    private void addVideoZoomCategory() {
        if (getPlayer() == null) {
            return;
        }

        addCategoryInt(AppDialogUtil.createVideoZoomCategory(
                getContext(), () -> {
                    getPlayer().setResizeMode(getPlayerData().getResizeMode());
                    getPlayer().setZoomPercents(getPlayerData().getZoomPercents());

                    // Make result easily be spotted by the user
                    getPlayer().showOverlay(false);
                }));
    }

    private void removeCategoryInt(int id) {
        mCategoriesInt.remove(id);
    }

    private void addCategoryInt(OptionCategory category) {
        mCategoriesInt.put(category.id, category);
    }

    public void removeCategory(int id) {
        mCategories.remove(id);
    }

    public void addCategory(OptionCategory category) {
        mCategories.put(category.id, category);
    }

    public void addOnDialogHide(Runnable listener) {
        mHideListeners.add(listener);
    }

    public void removeOnDialogHide(Runnable listener) {
        mHideListeners.remove(listener);
    }

    private void appendOptions(Map<Integer, OptionCategory> categories) {
        for (OptionCategory category : categories.values()) {
            mAppDialogPresenter.appendCategory(category);
        }
    }
}
