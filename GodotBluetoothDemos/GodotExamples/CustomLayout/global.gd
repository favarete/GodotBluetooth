extends Node

signal connecting
signal single_device_found
signal connected
signal disconnected
signal data_received

var bluetooth
var connecting = false setget _on_connecting

func _ready():
	if(Globals.has_singleton("GodotBluetooth")):
		bluetooth = Globals.get_singleton("GodotBluetooth")
		bluetooth.init(get_instance_ID(), false)
	pass

#Godot Simple Signal
func _on_connecting(new_boolean):
	connecting = new_boolean
	if new_boolean == true:
		emit_signal("connecting")
	pass

#GodotBluetooth Callbacks
func _on_connected(device_name, device_adress):
	connecting = false
	emit_signal("connected")
	pass

func _on_single_device_found(device_name, device_address, device_id):
	emit_signal("single_device_found", device_name, device_address, device_id)
	pass

func _on_disconnected():
	emit_signal("disconnected")
	pass

func _on_data_received(data_received):
	emit_signal("data_received", data_received)
	pass
