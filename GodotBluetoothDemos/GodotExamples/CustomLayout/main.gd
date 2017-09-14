extends Control

const btn_device = preload("res://btn_device.tscn")

onready var container = get_node("container")
onready var btn_connect = get_node("connection")
onready var btn_led = [ get_node("led_1"), get_node("led_2"), get_node("led_3") ]

func _ready():
	set_signals()
	pass

#Godot Simple Signal
func _on_connecting():
	clean_container()
	btn_connect.set_text("Connecting... Please Wait")
	pass

#Godot Simple Function
func clean_container():
	for node in container.get_children():
		if node extends Button:
			node.queue_free()
	pass

#GodotBluetooth Methods
func _on_connect_pressed():
	if global.bluetooth:
		clean_container()
		global.bluetooth.getPairedDevices(false)
	else:
		print("Module not initialized!")
	pass

func _on_btn_led_1_pressed():
	#Arduino is programmed to identify this string
	if global.bluetooth:
		global.bluetooth.sendData("led1")
	else:
		print("Module not initialized!")
	pass

func _on_btn_led_2_pressed():
	#Arduino is programmed to identify this string
	if global.bluetooth:
		global.bluetooth.sendData("led2")
	else:
		print("Module not initialized!")
	pass

func _on_btn_led_3_pressed():
	#Arduino is programmed to identify this string
	if global.bluetooth:
		global.bluetooth.sendData("led3")
	else:
		print("Module not initialized!")
	pass

#GodotBluetooth Callbacks
func _on_single_device_found(device_name, device_address, device_id):
	var new_device = btn_device.instance()
	new_device.device_index = device_id
	new_device.device_name = device_name
	new_device.device_address = device_address
	container.add_child(new_device)
	pass

func _on_connected():
	btn_connect.set_text("Disconnect")
	container.hide()
	for each in btn_led:
		each.show()
	pass
	
func _on_disconnected():
	btn_connect.set_text("Connect")
	container.show()
	for each in btn_led:
		each.hide()
	pass

func _on_data_received(data_received):
	if "l1on" in data_received:
		btn_led[0].set_text("Led 1 ON")
	else:
		btn_led[0].set_text("Led 1 OFF")
	if "l2on" in data_received:
		btn_led[1].set_text("Led 2 ON")
	else:
		btn_led[1].set_text("Led 2 OFF")
	if "l3on" in data_received:
		btn_led[2].set_text("Led 3 ON")
	else:
		btn_led[2].set_text("Led 3 OFF")
	pass

#Godot Signals
func set_signals():
	global.connect("single_device_found", self, "_on_single_device_found")
	global.connect("connected", self, "_on_connected")
	global.connect("disconnected", self, "_on_disconnected")
	global.connect("data_received", self, "_on_data_received")
	global.connect("connecting", self, "_on_connecting")

	btn_connect.connect("pressed", self, "_on_connect_pressed")
	btn_led[0].connect("pressed", self, "_on_btn_led_1_pressed")
	btn_led[1].connect("pressed", self, "_on_btn_led_2_pressed")
	btn_led[2].connect("pressed", self, "_on_btn_led_3_pressed")
	pass
