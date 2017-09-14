extends Button

var device_index
var device_name
var device_address

func _ready():
	connect("pressed", self, "_on_pressed")
	set_text(device_name)
	pass

func _on_pressed():
	if global.bluetooth:
		global.connecting = true
		global.bluetooth.connect(device_index)
	else:
		print("Module not initialized!")
	pass