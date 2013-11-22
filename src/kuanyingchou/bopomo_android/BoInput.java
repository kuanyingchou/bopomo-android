/*
 * Copyright (C) 2008-2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kuanyingchou.bopomo_android;

import android.content.res.AssetManager;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.text.InputType;
import android.text.method.MetaKeyKeyListener;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.CompletionInfo;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class BoInput extends InputMethodService 
        implements KeyboardView.OnKeyboardActionListener {
    
    private InputMethodManager mInputMethodManager;

    private BoKeyboardView mInputView;
    private BoCandidatesView mCandidateView;
    private CompletionInfo[] mCompletions;
    
    private StringBuilder mComposing = new StringBuilder();
    private boolean mPredictionOn;
    private boolean mCompletionOn;
    private int mLastDisplayWidth;
    private boolean mCapsLock;
    private long mLastShiftTime;
    private long mMetaState;
    
    private BoKeyboard mSymbolsKeyboard;
    private BoKeyboard mSymbolsShiftedKeyboard;
    private BoKeyboard mQwertyKeyboard;
    private BoKeyboard mBopomoKeyboard;
    
    private BoKeyboard mCurKeyboard;
    
    private String mWordSeparators;
    
    private BoWordTable wordTable;
    
    @Override public void onCreate() {
        super.onCreate();
        mInputMethodManager = 
                (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        mWordSeparators = getResources().getString(R.string.word_separators);

        loadWordTable();
    }

    private void loadWordTable() {
        final AssetManager am = getAssets();
        try {
            wordTable = new BoWordTable(am.open("phone.cin"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override public void onInitializeInterface() {
        if (mQwertyKeyboard != null) {
            // Configuration changes can happen after the keyboard gets recreated,
            // so we need to be able to re-build the keyboards if the available
            // space has changed.
            int displayWidth = getMaxWidth();
            if (displayWidth == mLastDisplayWidth) return;
            mLastDisplayWidth = displayWidth;
        }
        mQwertyKeyboard = new BoKeyboard(this, R.xml.qwerty);
        mSymbolsKeyboard = new BoKeyboard(this, R.xml.symbols);
        mSymbolsShiftedKeyboard = new BoKeyboard(this, R.xml.symbols_shift);
        mBopomoKeyboard = new BoKeyboard(this, R.xml.bopomo);
    }
    
    @Override public View onCreateInputView() {
        mInputView = (BoKeyboardView) getLayoutInflater().inflate(
                R.layout.input, null); //>>>
        mInputView.setOnKeyboardActionListener(this);
        mInputView.setKeyboard(mBopomoKeyboard);
        return mInputView;
    }

    @Override public View onCreateCandidatesView() {
        mCandidateView = new BoCandidatesView(this);
        mCandidateView.setService(this);
        return mCandidateView; }

    /**
     * This is the main point where we do our initialization of the input method
     * to begin operating on an application.  At this point we have been
     * bound to the client, and are now receiving all of the detailed information
     * about the target of our edits.
     */
    @Override public void onStartInput(EditorInfo attribute, boolean restarting) {
        super.onStartInput(attribute, restarting);
        
        // Reset our state.  We want to do this even if restarting, because
        // the underlying state of the text editor could have changed in any way.
        reset(restarting);
        
        // We are now going to initialize our state based on the type of
        // text being edited.
        chooseKeyboard(attribute);

    }

    private void reset(boolean restarting) {
        mComposing.setLength(0);
        updateCandidates();
        
        if (!restarting) {
            // Clear shift states.
            mMetaState = 0;
        }
        
        mPredictionOn = false;
        mCompletionOn = false;
        mCompletions = null;
    }

    private void chooseKeyboard(EditorInfo attribute) {
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
                // Numbers and dates default to the symbols keyboard, with
                // no extra features.
                mCurKeyboard = mSymbolsKeyboard;
                break;
                
            case InputType.TYPE_CLASS_PHONE:
                // Phones will also default to the symbols keyboard, though
                // often you will want to have a dedicated phone keyboard.
                mCurKeyboard = mSymbolsKeyboard;
                break;
                
            case InputType.TYPE_CLASS_TEXT:
                // This is general text editing.  We will default to the
                // normal alphabetic keyboard, and assume that we should
                // be doing predictive text (showing candidates as the
                // user types).
                mCurKeyboard = mBopomoKeyboard; //mQwertyKeyboard;
                mPredictionOn = true;
                
                // We now look for a few special variations of text that will
                // modify our behavior.
                int variation = attribute.inputType & InputType.TYPE_MASK_VARIATION;
                if (variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
                        variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) {
                    // Do not display predictions / what the user is typing
                    // when they are entering a password.
                    mPredictionOn = false;
                }
                
                if (variation == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                        || variation == InputType.TYPE_TEXT_VARIATION_URI
                        || variation == InputType.TYPE_TEXT_VARIATION_FILTER) {
                    // Our predictions are not useful for e-mail addresses
                    // or URIs.
                    mPredictionOn = false;
                }
                
                if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    // If this is an auto-complete text view, then our predictions
                    // will not be shown and instead we will allow the editor
                    // to supply their own.  We only show the editor's
                    // candidates when in fullscreen mode, otherwise relying
                    // own it displaying its own UI.
                    mPredictionOn = false;
                    mCompletionOn = isFullscreenMode();
                }
                
                // We also want to look at the current state of the editor
                // to decide whether our alphabetic keyboard should start out
                // shifted.
                updateShiftKeyState(attribute);
                break;
                
            default:
                // For all unknown input types, default to the alphabetic
                // keyboard with no special features.
                mCurKeyboard = mQwertyKeyboard;
                updateShiftKeyState(attribute);
        }
        
        // Update the label on the enter key, depending on what the application
        // says it will do.
        mCurKeyboard.setImeOptions(getResources(), attribute.imeOptions);
    }

    /**
     * This is called when the user is done editing a field.  We can use
     * this to reset our state.
     */
    @Override public void onFinishInput() {
        super.onFinishInput();
        
        // Clear current composing text and candidates.
        mComposing.setLength(0);
        updateCandidates();
        
        // We only hide the candidates window when finishing input on
        // a particular editor, to avoid popping the underlying application
        // up and down if the user is entering text into the bottom of
        // its window.
        setCandidatesViewShown(false);
        
        mCurKeyboard = mQwertyKeyboard;
        if (mInputView != null) {
            mInputView.closing();
        }
    }
    
    @Override public void onStartInputView(EditorInfo attribute, boolean restarting) {
        super.onStartInputView(attribute, restarting);
        // Apply the selected keyboard to the input view.
        mInputView.setKeyboard(mCurKeyboard);
        mInputView.closing();
        final InputMethodSubtype subtype = mInputMethodManager.getCurrentInputMethodSubtype();
        mInputView.setSubtypeOnSpaceKey(subtype);
    }

    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        mInputView.setSubtypeOnSpaceKey(subtype);
    }

    /**
     * Deal with the editor reporting movement of its cursor.
     */
    @Override public void onUpdateSelection(
            int oldSelStart, int oldSelEnd,
            int newSelStart, int newSelEnd,
            int candidatesStart, int candidatesEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd,
                candidatesStart, candidatesEnd);
        
        // If the current selection in the text view changes, we should
        // clear whatever candidate text we have.
        if (mComposing.length() > 0 && (newSelStart != candidatesEnd
                || newSelEnd != candidatesEnd)) {
            mComposing.setLength(0);
            updateCandidates();
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) {
                ic.finishComposingText();
            }
        }
    }

    /**
     * This tells us about completions that the editor has determined based
     * on the current text in it.  We want to use this in fullscreen mode
     * to show the completions ourself, since the editor can not be seen
     * in that situation.
     */
    @Override public void onDisplayCompletions(CompletionInfo[] completions) {
        if (mCompletionOn) {
            mCompletions = completions;
            if (completions == null) {
                setSuggestions(null, false, false);
                return;
            }
            
            List<String> stringList = new ArrayList<String>();
            for (int i = 0; i < completions.length; i++) {
                CompletionInfo ci = completions[i];
                if (ci != null) stringList.add(ci.getText().toString());
            }
            setSuggestions(stringList, true, true);
        }
    }
    
    //[ private

    /**
     * Helper function to commit any text being composed in to the editor.
     */
    private void commitTyped(InputConnection inputConnection) {
        if (mComposing.length() > 0) {
            final List<BoWord> candidates = wordTable.get(mComposing.toString()); //>>> strange, should get the first char from candidatesView
            if(candidates.size() <= 0) return;
            inputConnection.commitText(candidates.get(0).toString(), candidates.get(0).toString().length());
            mComposing.setLength(0);
            updateCandidates();
        }
    }

    /**
     * Helper to update the shift state of our keyboard based on the initial
     * editor state.
     */
    private void updateShiftKeyState(EditorInfo attr) {
        if (attr != null 
                && mInputView != null && mQwertyKeyboard == mInputView.getKeyboard()) {
            int caps = 0;
            EditorInfo ei = getCurrentInputEditorInfo();
            if (ei != null && ei.inputType != InputType.TYPE_NULL) {
                caps = getCurrentInputConnection().getCursorCapsMode(attr.inputType);
            }
            mInputView.setShifted(mCapsLock || caps != 0);
        }
    }
    
    /**
     * Helper to determine if a given character code is alphabetic.
     */
    private boolean isAlphabet(int code) {
        if (Character.isLetter(code)) {
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }


    /**
     * Helper to send a character to the editor as raw key events.
     */
    public void sendString(String input) {
        for(int i=0; i<input.length(); i++) {
            sendKey(input.charAt(i));
        }
        clearComposing();
    }
    private void sendKey(int keyCode) {
        switch (keyCode) {
            case '\n':
                keyDownUp(KeyEvent.KEYCODE_ENTER);
                break;
            default:
                if (keyCode >= '0' && keyCode <= '9') {
                    keyDownUp(keyCode - '0' + KeyEvent.KEYCODE_0);
                } else {
                    getCurrentInputConnection().commitText(String.valueOf((char) keyCode), 1);
                }
                break;
        }
    }

    //[ implements KeyboardView.OnKeyboardActionListener
    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
System.out.println("onKey: "+primaryCode);
        if (isSpace(primaryCode)) {
            // Handle separator
            if (mComposing.length() > 0) {
                commitTyped(getCurrentInputConnection());
            } else {
                sendKey(primaryCode);
            }
        } else if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            handleClose();
            return;
        } else if (primaryCode == BoKeyboardView.KEYCODE_OPTIONS) {
            // Show a menu or somethin'
        } else if (primaryCode == Keyboard.KEYCODE_MODE_CHANGE
                && mInputView != null) {
            final Keyboard current = mInputView.getKeyboard();
            if (current == mSymbolsKeyboard) {
                switchKeyboard(mBopomoKeyboard);
            } else {
                switchKeyboard(mSymbolsKeyboard);
            }
            if (mCurKeyboard == mSymbolsKeyboard) {
                current.setShifted(false);
            }
        } else {
            handleCharacter(primaryCode, keyCodes);
        }
    }

    private void switchKeyboard(BoKeyboard kb) {
        if (mComposing.length() > 0) {
            commitTyped(getCurrentInputConnection());
        }
        reset(false);
        mCurKeyboard = kb;
        mInputView.setKeyboard(mCurKeyboard);
    }

    @Override
    public void onText(CharSequence text) {
System.out.println("onText: "+text);
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        if (mComposing.length() > 0) {
            commitTyped(ic);
        }
        ic.commitText(text, 0);
        ic.endBatchEdit();
        updateShiftKeyState(getCurrentInputEditorInfo());
    }

    @Override
    public void swipeRight() {
    }
    
    @Override
    public void swipeLeft() {
    }

    @Override
    public void swipeDown() {
    }

    @Override
    public void swipeUp() {
    }
    
    @Override
    public void onPress(int primaryCode) {
    }
    
    @Override
    public void onRelease(int primaryCode) {
    }
    //] implements KeyboardView.OnKeyboardActionListener

    /**
     * Update the list of available candidates from the current composing
     * text.  This will need to be filled in by however you are determining
     * candidates.
     */
    private boolean canCompose(String s) {
        String test = mComposing.toString() + s; 
        final List<BoWord> words = wordTable.get(test);
        if(words.size() <= 1) {
            words.addAll(wordTable.getPossible(test, 100));
        }
        return words.size() > 0;
    }
    private void updateCandidates() {
        //if (!mCompletionOn) {
            if (mComposing.length() > 0) {
                final List<BoWord> words = wordTable.get(mComposing.toString());
                if(words.size() <= 1) {
                    words.addAll(wordTable.getPossible(mComposing.toString(), 100));
                }
                //>>>
                final List<String> list = new ArrayList<String>();
                for(BoWord w : words) {
                    list.add(w.toString());
                }
System.out.println("candidates for "+mComposing+": "+BoWordTable.join(list, ", "));                
                setSuggestions(list, true, true);
            } else {
                setSuggestions(null, false, false);
            }
        //}
    }
    
    private void setSuggestions(List<String> suggestions, boolean completions,
            boolean typedWordValid) {
        if (suggestions != null && suggestions.size() > 0) {
            setCandidatesViewShown(true);
        }         
        if (mCandidateView != null) {
            mCandidateView.setCandidates(suggestions);
        }
    }
    
    private void handleBackspace() {
        final int length = mComposing.length();
        if (length > 1) {
            deleteComposing();
        } else if (length == 1) {
            clearComposing();
            getCurrentInputConnection().commitText("", 0);
        } else {
            getCurrentInputConnection().commitText("", 0);
            keyDownUp(KeyEvent.KEYCODE_DEL);
        }
    }

    private void handleShift() {
        if (mInputView == null) {
            return;
        }
        
        Keyboard currentKeyboard = mInputView.getKeyboard();
        if (mQwertyKeyboard == currentKeyboard) {
            // Alphabet keyboard
            checkToggleCapsLock();
            mInputView.setShifted(mCapsLock || !mInputView.isShifted());
        } else if (currentKeyboard == mSymbolsKeyboard) {
            mSymbolsKeyboard.setShifted(true);
            mInputView.setKeyboard(mSymbolsShiftedKeyboard);
            mSymbolsShiftedKeyboard.setShifted(true);
        } else if (currentKeyboard == mSymbolsShiftedKeyboard) {
            mSymbolsShiftedKeyboard.setShifted(false);
            mInputView.setKeyboard(mSymbolsKeyboard);
            mSymbolsKeyboard.setShifted(false);
        }
    }
    
    private void handleCharacter(int primaryCode, int[] keyCodes) {
        final InputConnection ic = getCurrentInputConnection();

        if(mCurKeyboard == mSymbolsKeyboard) {
            ic.commitText(String.valueOf((char)primaryCode), 1);
            return;
        }
        
        if((char)primaryCode == ' ') {
System.out.println("space pressed");
            confirmComposing();
        } else {        
            final String bopomoKey = wordTable.getKeyName(
                    String.valueOf((char)primaryCode));
System.out.println("current key: " + bopomoKey + "("+primaryCode+")");

            if(canCompose(bopomoKey)) {
                appendComposing(bopomoKey);
            }
        }
    }

    private void handleClose() {
        commitTyped(getCurrentInputConnection());
        requestHideSelf(0);
        mInputView.closing();
    }

    private void checkToggleCapsLock() {
        long now = System.currentTimeMillis();
        if (mLastShiftTime + 800 > now) {
            mCapsLock = !mCapsLock;
            mLastShiftTime = 0;
        } else {
            mLastShiftTime = now;
        }
    }
    
    private String getWordSeparators() {
        return mWordSeparators;
    }

    private boolean isSpace(int keyCode) {
        return (char)keyCode == ' ';
    }
    
    public static boolean isWordSeparator(int code) {
        String separators = " "; //getWordSeparators();
        return separators.contains(String.valueOf((char)code));
    }

    @Override public void onWindowHidden() {
        setCandidatesViewShown(false);
        Log.d("ken", "onWindowHidden");
    }
    
    private void appendComposing(String s) {
        mComposing.append(s);
        updateCandidates();
        getCurrentInputConnection().setComposingText(mComposing, 1);
    }
    private void deleteComposing() {
        final int length = mComposing.length();
        mComposing.delete(length - 1, length);
        updateCandidates();
        getCurrentInputConnection().setComposingText(mComposing, 1);
    }
    private void clearComposing() {
        mComposing.setLength(0);
        updateCandidates();
        getCurrentInputConnection().commitText("", 0);
    }
    private void confirmComposing() {
        getCurrentInputConnection().finishComposingText();
        clearComposing();
    }
    
}
