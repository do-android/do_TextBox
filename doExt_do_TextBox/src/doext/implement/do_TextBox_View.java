package doext.implement;

import java.lang.reflect.Field;
import java.util.Map;

import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import core.DoServiceContainer;
import core.helper.DoJsonHelper;
import core.helper.DoTextHelper;
import core.helper.DoUIModuleHelper;
import core.interfaces.DoIScriptEngine;
import core.interfaces.DoIUIModuleView;
import core.object.DoInvokeResult;
import core.object.DoUIModule;
import doext.define.do_TextBox_IMethod;
import doext.define.do_TextBox_MAbstract;

/**
 * 自定义扩展UIView组件实现类，此类必须继承相应VIEW类，并实现DoIUIModuleView,Do_TextBox_IMethod接口；
 * #如何调用组件自定义事件？可以通过如下方法触发事件：
 * this.model.getEventCenter().fireEvent(_messageName, jsonResult);
 * 参数解释：@_messageName字符串事件名称，@jsonResult传递事件参数对象； 获取DoInvokeResult对象方式new
 * DoInvokeResult(this.model.getUniqueKey());
 */
public class do_TextBox_View extends EditText implements DoIUIModuleView, do_TextBox_IMethod, OnTouchListener, OnFocusChangeListener {
	private static final String INPUT_TYPE_ASC = "ASC"; // 支持ASCII的默认键盘
	private static final String INPUT_TYPE_PHONENUMBER = "PHONENUMBER"; // 标准电话键盘，支持＋＊＃字符
	private static final String INPUT_TYPE_URL = "URL"; // URL键盘，支持.com按钮  只支持URL字符
	private static final String INPUT_TYPE_DECIMAL = "DECIMAL"; // 数字与小数点键盘

	/**
	 * 每个UIview都会引用一个具体的model实例；
	 */
	private do_TextBox_MAbstract model;

	public do_TextBox_View(Context context) {
		super(context);
		this.setSingleLine(false);
		this.setPadding(1, 0, 1, 0);
		this.setGravity(Gravity.TOP | Gravity.LEFT);
		this.setBackgroundDrawable(null);
		this.setFilters(new InputFilter[] { new InputFilter.LengthFilter(100) });
	}

	/**
	 * 初始化加载view准备,_doUIModule是对应当前UIView的model实例
	 */
	@Override
	public void loadView(DoUIModule _doUIModule) throws Exception {
		this.model = (do_TextBox_MAbstract) _doUIModule;
		this.setTextSize(TypedValue.COMPLEX_UNIT_PX, DoUIModuleHelper.getDeviceFontSize(_doUIModule, "17"));
		this.addTextChangedListener(textWatcher);
		// 设置焦点改变的监听
		setOnFocusChangeListener(this);
		setOnTouchListener(this);
	}

	/**
	 * 动态修改属性值时会被调用，方法返回值为true表示赋值有效，并执行onPropertiesChanged，否则不进行赋值；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public boolean onPropertiesChanging(Map<String, String> _changedValues) {
		return true;
	}

	/**
	 * 属性赋值成功后被调用，可以根据组件定义相关属性值修改UIView可视化操作；
	 * 
	 * @_changedValues<key,value>属性集（key名称、value值）；
	 */
	@Override
	public void onPropertiesChanged(Map<String, String> _changedValues) {
		DoUIModuleHelper.handleBasicViewProperChanged(this.model, _changedValues);
		if (_changedValues.containsKey("enabled")) {
			// enabled设置为false的时候 首次加载 还是会触发一次focuschange事件 所以在这里做一下处理
			this.setEnabled(Boolean.parseBoolean(_changedValues.get("enabled")));
			this.setFocusable(Boolean.parseBoolean(_changedValues.get("enabled")));
		}
		if (_changedValues.containsKey("hint")) {
			this.setHint(_changedValues.get("hint"));
		}
		if (_changedValues.containsKey("hintColor")) {
			this.setHintTextColor(DoUIModuleHelper.getColorFromString(_changedValues.get("hintColor"), Color.parseColor("#808080")));
		}
		if (_changedValues.containsKey("maxLength")) {
			int imaxLength = DoTextHelper.strToInt(_changedValues.get("maxLength"), 100);
			if (imaxLength >= 0) {
				this.setFilters(new InputFilter[] { new InputFilter.LengthFilter(imaxLength) });
			}
		}

		// 最大行数
		if (_changedValues.containsKey("maxLines")) {
			int maxLines = DoTextHelper.strToInt(_changedValues.get("maxLines"), 0);
			if (maxLines > 0) {// 缺省为0行，表示不限制行数
				this.setMaxLines(maxLines);
			}
		}

		DoUIModuleHelper.setFontProperty(this.model, _changedValues);
		if (_changedValues.containsKey("inputType")) {
			String value = _changedValues.get("inputType");
			if (value.equals(INPUT_TYPE_ASC)) {
				this.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
			} else if (value.equals(INPUT_TYPE_PHONENUMBER)) {
				this.setInputType(InputType.TYPE_CLASS_PHONE);
			} else if (value.equals(INPUT_TYPE_URL)) {
				this.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
			} else if (value.equals(INPUT_TYPE_DECIMAL)) {
				this.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
			} else {
				this.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS | InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
			}
		}
		if (_changedValues.containsKey("text")) {
			this.setSelection(this.getText().length());
		}
		if (_changedValues.containsKey("cursorColor")) {
			setCursorDrawableColor(DoUIModuleHelper.getColorFromString(_changedValues.get("cursorColor"), Color.BLACK));
		}
	}

	private void setCursorDrawableColor(int color) {
		try {
			Field fCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
			fCursorDrawableRes.setAccessible(true);
			int mCursorDrawableRes = fCursorDrawableRes.getInt(this);
			Field fEditor = TextView.class.getDeclaredField("mEditor");
			fEditor.setAccessible(true);
			Object editor = fEditor.get(this);
			Class<?> clazz = editor.getClass();
			Field fCursorDrawable = clazz.getDeclaredField("mCursorDrawable");
			fCursorDrawable.setAccessible(true);
			Drawable[] drawables = new Drawable[2];
			drawables[0] = this.getContext().getResources().getDrawable(mCursorDrawableRes);
			drawables[1] = this.getContext().getResources().getDrawable(mCursorDrawableRes);
			drawables[0].setColorFilter(color, PorterDuff.Mode.SRC_IN);
			drawables[1].setColorFilter(color, PorterDuff.Mode.SRC_IN);
			fCursorDrawable.set(editor, drawables);
		} catch (Throwable ignored) {
		}
	}

	/**
	 * 同步方法，JS脚本调用该组件对象方法时会被调用，可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前Page JS上下文环境对象
	 * @_invokeResult 用于返回方法结果对象
	 */
	@Override
	public boolean invokeSyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		if ("setFocus".equals(_methodName)) {
			this.setFocus(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		if ("setSelection".equals(_methodName)) {
			this.setSelection(_dictParas, _scriptEngine, _invokeResult);
			return true;
		}
		return false;
	}

	/**
	 * 异步方法（通常都处理些耗时操作，避免UI线程阻塞），JS脚本调用该组件对象方法时会被调用， 可以根据_methodName调用相应的接口实现方法；
	 * 
	 * @_methodName 方法名称
	 * @_dictParas 参数（K,V）
	 * @_scriptEngine 当前page JS上下文环境
	 * @_callbackFuncName 回调函数名 #如何执行异步方法回调？可以通过如下方法：
	 *                    _scriptEngine.callback(_callbackFuncName,
	 *                    _invokeResult);
	 *                    参数解释：@_callbackFuncName回调函数名，@_invokeResult传递回调函数参数对象；
	 *                    获取DoInvokeResult对象方式new
	 *                    DoInvokeResult(this.model.getUniqueKey());
	 */
	@Override
	public boolean invokeAsyncMethod(String _methodName, JSONObject _dictParas, DoIScriptEngine _scriptEngine, String _callbackFuncName) {
		return false;
	}

	private void setFocus(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		boolean _inputFlag = DoJsonHelper.getBoolean(_dictParas, "value", false); // true
		InputMethodManager _imm = ((InputMethodManager) this.getContext().getSystemService(Context.INPUT_METHOD_SERVICE));
		Activity _activity = DoServiceContainer.getPageViewFactory().getAppContext();
		if (_inputFlag) {
			this.setFocusable(true);
			this.setFocusableInTouchMode(true);
			this.requestFocus();
			this.findFocus();
			_imm.toggleSoftInput(0, InputMethodManager.SHOW_IMPLICIT);
			_activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		} else {
			this.setFocusable(false);
			this.clearFocus();
			_imm.hideSoftInputFromWindow(this.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
			_activity.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		}
	}

	private void setSelection(JSONObject _dictParas, DoIScriptEngine _scriptEngine, DoInvokeResult _invokeResult) throws Exception {
		int _len = DoJsonHelper.getInt(_dictParas, "position", 0);
		if (_len < 0) {
			_len = 0;
		}
		int _textLen = this.getText().length();
		if (_len > _textLen) {
			_len = _textLen;
		}
		this.setSelection(_len);
	}

	/**
	 * 释放资源处理，前端JS脚本调用closePage或执行removeui时会被调用；
	 */
	@Override
	public void onDispose() {
	}

	private void doTextBoxView_TextChanged() {
		if (this.model.getCurrentPage().getScriptEngine() != null) { // 去除脚本还未加载时NULL的情况
			DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
			this.model.getEventCenter().fireEvent("textChanged", _invokeResult);
		}
	}

	TextWatcher textWatcher = new TextWatcher() {
		@Override
		public void afterTextChanged(Editable arg0) {
		}

		@Override
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
		}

		@Override
		public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
			// 解决联想手机（4.2.1）动态改变hintColor不起作用的问题
			String _text = getText().toString();
			if (_text == null || _text.length() == 0) {
				try {
					setHintTextColor(DoUIModuleHelper.getColorFromString(model.getPropertyValue("hintColor"), Color.parseColor("#808080")));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			model.setPropertyValue("text", _text);
			doTextBoxView_TextChanged();
		}
	};

	/**
	 * 重绘组件，构造组件时由系统框架自动调用；
	 * 或者由前端JS脚本调用组件onRedraw方法时被调用（注：通常是需要动态改变组件（X、Y、Width、Height）属性时手动调用）
	 */
	@Override
	public void onRedraw() {
		this.setLayoutParams(DoUIModuleHelper.getLayoutParams(this.model));
	}

	/**
	 * 获取当前model实例
	 */
	@Override
	public DoUIModule getModel() {
		return model;
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		DoInvokeResult _invokeResult = new DoInvokeResult(this.model.getUniqueKey());
		this.model.getEventCenter().fireEvent(hasFocus ? "focusIn" : "focusOut", _invokeResult);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (!isEnabled()) {
			return false;
		}
		return super.onTouchEvent(event);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			setFocusable(true);
			setFocusableInTouchMode(true);
			requestFocus();
			findFocus();
		}
		return false;
	}
}
