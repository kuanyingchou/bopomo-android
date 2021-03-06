package org.wen_input;

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

// An input service receives hard or soft keyboard 
// events, translates the events into characters, and
// fills in the characters to an input edit.

public class WenInputService extends InputMethodService 
        implements KeyboardView.OnKeyboardActionListener {
    
    private KeyboardView mInputView;
    private WenCandidatesView mCandidateView;
    
    private Composing mComposing = new Composing();
    private boolean mCompletionOn;
    private int mLastDisplayWidth;
    private boolean mCapsLock;
    private long mLastShiftTime;
    
    private WenKeyboard mSymbolsKeyboard;
    private WenKeyboard mSymbolsShiftedKeyboard;
    private WenKeyboard mQwertyKeyboard;
    private WenKeyboard mBopomoKeyboard;
    
    private WenKeyboard mCurKeyboard;
    
    private WenWordTable bopomoTable;
    private WenPhraseTable phraseTable; 
    
    @Override public void onCreate() {
        super.onCreate();
        loadBopomoTable();
    }

    private void loadBopomoTable() {
        final AssetManager am = getAssets();
        try {
            bopomoTable = new WenWordTable(
                    am.open("phone.cin"), 
                    am.open("freq-2013-11-26-sorted"));
            phraseTable = new WenPhraseTable(am.open("tsi.src.sorted"));
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
        mQwertyKeyboard = new WenKeyboard(this, R.xml.qwerty);
        mSymbolsKeyboard = new WenKeyboard(this, R.xml.symbols);
        mSymbolsShiftedKeyboard = new WenKeyboard(this, R.xml.symbols_shift);
        mBopomoKeyboard = new WenKeyboard(this, R.xml.bopomo);
    }
    
    @Override public View onCreateInputView() {
        mInputView = (KeyboardView) getLayoutInflater().inflate(
                R.layout.input, null); //>>>
        mInputView.setOnKeyboardActionListener(this);
        mInputView.setKeyboard(mBopomoKeyboard); //>>> change to default keyboard
        return mInputView;
    }

    @Override public View onCreateCandidatesView() {
        mCandidateView = new WenCandidatesView(this);
        return mCandidateView; 
    }
    

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
        mComposing.reset();
        updateCandidates();
        
        mCompletionOn = false;
    }

    private void chooseKeyboard(EditorInfo attribute) {
        switch (attribute.inputType & InputType.TYPE_MASK_CLASS) {
            case InputType.TYPE_CLASS_NUMBER:
            case InputType.TYPE_CLASS_DATETIME:
                mCurKeyboard = mSymbolsKeyboard;
                break;
            case InputType.TYPE_CLASS_PHONE:
                mCurKeyboard = mSymbolsKeyboard;
                break;
            case InputType.TYPE_CLASS_TEXT:
                mCurKeyboard = mBopomoKeyboard; //mQwertyKeyboard;
                
                if ((attribute.inputType & InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE) != 0) {
                    mCompletionOn = isFullscreenMode();
                }
                
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
        mComposing.reset();
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
    }

    @Override
    public void onCurrentInputMethodSubtypeChanged(InputMethodSubtype subtype) {
        super.onCurrentInputMethodSubtypeChanged(subtype);
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
        if (mComposing.length() > 0 && 
                (newSelStart != candidatesEnd || 
                   newSelEnd != candidatesEnd)) {
            confirmComposing();
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
            
            final List<WenWord> candidates = bopomoTable.get(mComposing.toString()); //>>> strange, should get the first char from candidatesView
            if(candidates.size() <= 0) return;
            inputConnection.commitText(candidates.get(0).toString(), candidates.get(0).toString().length());
            mComposing.reset();
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
    
    
    //[ hard key events 
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        super.onKeyDown(keyCode, event);
        onKey(keyCode, null);
        return true;
    }
    
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        return super.onKeyUp(keyCode, event);
    }
    //]
    
    /**
     * Helper to send a key down / key up pair to the current editor.
     */
    private void keyDownUp(int keyEventCode) {
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
        getCurrentInputConnection().sendKeyEvent(
                new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
    }


    public void sendString(String input) {
        for(int i=0; i<input.length(); i++) {
            sendKey(input.charAt(i));
        }
        clearComposing();
        
        final String last = String.valueOf(input.charAt(input.length()-1));
        if (mCandidateView != null && phraseTable != null) {
            List<WenPhrase> candidates = phraseTable.get(last);
            List<String> trimmed = new ArrayList<String>();
            int size = Math.min(candidates.size(), 20); 
            for(int i=0; i<size; i++) {
                System.out.println(WenUtil.join(candidates, ", "));
                trimmed.add(candidates.get(i).value.substring(1));
            }
            mCandidateView.setCandidates(trimmed);
        }
    }

    /**
     * Helper to send a character to the editor as raw key events.
     */
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

    //[ soft key events,
    //  implements KeyboardView.OnKeyboardActionListener
    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        //System.out.println("onKey: "+primaryCode);
        if (isSpace(primaryCode)) {
            // Handle separator
            if (mComposing.length() > 0) {
                //commitTyped(getCurrentInputConnection());
                if(mCandidateView.hasCandidates()) {
                    sendString(mCandidateView.getFirstCandidate().toString());    
                } else {
                    sendString(mComposing.toString());
                }
            } else {
                sendKey(primaryCode);
            }
        } else if (primaryCode == Keyboard.KEYCODE_DELETE) {
            handleBackspace();
        } else if (primaryCode == Keyboard.KEYCODE_CANCEL) {
            handleClose();
            return;
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

    private void switchKeyboard(WenKeyboard kb) {
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
        String test = compose(mComposing.toString(), s); 
        final List<WenWord> words = bopomoTable.get(test);
        if(words.size() <= 1) {
            words.addAll(bopomoTable.getPossible(test, 20));
        }
        return words.size() > 0;
    }
    
    private static class Composing {
        char prefix;
        char middle;
        char suffix;
        char modifier;
        
        public void reset() {
            prefix = middle = suffix = modifier = 0;
        }
        public int length() {
            return toString().length();
        }
        private void add(char c) {
            if(isPrefix(c)) {
                prefix = c;
            } else if(isMiddle(c)) {
                middle = c;
            } else if(isSuffix(c)) {
                suffix = c;
            } else if(isModifier(c)) {
                modifier = (modifier == c)? 0: c;
            } else {
                //what?
            }
        }
        public void add(String s) {
            for(int i=0; i<s.length(); i++) {
                add(s.charAt(i));
            }
        }
        public void set(String s) {
            reset();
            add(s);
        }
        public void delete() {
            if(modifier != 0) modifier = 0;
            else if(suffix != 0) suffix = 0;
            else if(middle != 0) middle = 0;
            else if(prefix != 0) prefix = 0;
            else ; //nothing to delete
        }
        private static boolean isPrefix(char c) {
            return "ㄅㄆㄇㄈㄉㄊㄋㄌㄍㄎㄏㄐㄑㄒㄓㄔㄕㄖㄗㄘㄙ".indexOf(c) != -1 ;
        }
        private static boolean isSuffix(char c) {
            return "ㄚㄛㄜㄝㄞㄟㄠㄡㄢㄣㄤㄥㄦ".indexOf(c) != -1;
        }
        private static boolean isMiddle(char c) {
            return "ㄧㄨㄩ".indexOf(c) != -1;
        }
        private static boolean isModifier(char c) {
            return "ˊˇˋ˙".indexOf(c) != -1;
        }
        private String toString(char c) {
            if(c == 0) return "";
            else return String.valueOf(c);
        }
        public String toString() {
            return (""+toString(prefix) + toString(middle) + 
                    toString(suffix) + toString(modifier));
        }
        
    }

    private String compose(String origin, String addon) {
        final char a = addon.charAt(0);
        mComposing = new Composing();
        mComposing.set(origin);
        mComposing.add(addon);
        return mComposing.toString();
        
    }

    private void updateCandidates() {
        //if (!mCompletionOn) {
            if (mComposing.length() > 0) {
                final List<WenWord> words = bopomoTable.get(mComposing.toString());
                if(words.size() <= 1) {
                    words.addAll(bopomoTable.getPossible(mComposing.toString(), 100));
                }
                //>>>
                final List<String> list = new ArrayList<String>();
                for(WenWord w : words) {
                    list.add(w.toString());
                }
System.out.println("candidates for "+mComposing+": "+WenUtil.join(list, ", "));                
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
            final String bopomoKey = bopomoTable.getKeyName(
                    String.valueOf((char)primaryCode));
System.out.println("current key: " + bopomoKey + "("+primaryCode+")");

            //if(canCompose(bopomoKey)) {
                setComposing(compose(mComposing.toString(), bopomoKey));
            //}
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

    private boolean isSpace(int keyCode) {
        return (char)keyCode == ' ';
    }
    
    @Override public void onWindowHidden() {
        setCandidatesViewShown(false);
        Log.d("ken", "onWindowHidden");
    }

    private void setComposing(String s) {
        mComposing.set(s);
        updateCandidates();
        getCurrentInputConnection().setComposingText(mComposing.toString(), 1);
    }
    private void appendComposing(String s) {
        mComposing.add(s);
        updateCandidates();
        getCurrentInputConnection().setComposingText(mComposing.toString(), 1);
    }
    private void deleteComposing() {
        final int length = mComposing.length();
        mComposing.delete();
        updateCandidates();
        getCurrentInputConnection().setComposingText(mComposing.toString(), 1);
    }
    private void clearComposing() {
        mComposing.reset(); 
        updateCandidates();
        getCurrentInputConnection().commitText("", 0);
    }
    private void confirmComposing() {
        getCurrentInputConnection().finishComposingText();
        clearComposing();
    }
    
}
