# FollowFingerLayout
Android :help for you use windowmanager with animation 

# usage

    val root = LayoutInflater.from(applicationContext).inflate(R.layout.layout_window_player, null)
    val fingerLayout = FollowFingerLayout(root)
    val params: WindowManager.LayoutParams = WindowManager.LayoutParams()
    params.width = ViewGroup.LayoutParams.WRAP_CONTENT
    params.height = UiUtil.dp2px(applicationContext, R.dimen.player_wrap_height)
    params.gravity = Gravity.END.or(Gravity.BOTTOM)
    params.type = WindowManager.LayoutParams.TYPE_PHONE
    params.flags = WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS.or(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
    params.format = PixelFormat.RGBA_8888

    fingerLayout.attachToWindow(params)
