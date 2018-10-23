extends Control

onready var btn_connect = get_node("connection")
onready var btn_led = [ get_node("builtin_led") ]

func _ready():
	set_signals()
	pass

#GodotBluetooth Methods
func _on_connect_pressed():
	if global.bluetooth:
		global.bluetooth.getPairedDevices(true)
	else:
		print("Module not initialized!")
	pass

func _on_btn_builtin_led_pressed():
	#Arduino is programmed to identify this string
	if global.bluetooth:
		global.bluetooth.sendData("builtin_led")
	else:
		print("Module not initialized!")
	pass

#GodotBluetooth Callbacks
func _on_connected():
	btn_connect.set_text("Disconnect")
	for each in btn_led:
		each.show()
	pass

func _on_disconnected():
	btn_connect.set_text("Connect")
	for each in btn_led:
		each.hide()
	pass

func _on_data_received(data_received):
	if "ledon" in data_received:
		btn_led[0].set_text("Led ON")
	else:
		btn_led[0].set_text("Led OFF")
	pass

#Godot Signals
func set_signals():
	global.connect("connected", self, "_on_connected")
	global.connect("disconnected", self, "_on_disconnected")
	global.connect("data_received", self, "_on_data_received")

	btn_connect.connect("pressed", self, "_on_connect_pressed")
	btn_led[0].connect("pressed", self, "_on_btn_builtin_led_pressed")
	pass
