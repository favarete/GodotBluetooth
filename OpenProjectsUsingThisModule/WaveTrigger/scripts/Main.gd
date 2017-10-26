extends Node

var accelerometer_x
var sensor = false
var sensibility = 0
var wait = false
var call_effect = false

onready var btn_connect = get_node("Home/connect")
onready var btn_disconnect = get_node("Home/disconnect")
onready var anim = get_node("Home/anim")

const trigger_pointer_tex = [   preload("res://sprites/pointer.png"),
								preload("res://sprites/pointerenergy.png")]
const trigger_display_tex = [   preload("res://sprites/display.png"),
								preload("res://sprites/displaye.png")]
const effect_sprite = [ preload("res://scenes/noeffect.tscn"),
						preload("res://scenes/tremolo.tscn"),
						preload("res://scenes/distortion.tscn")]
onready var btn_effects = [ get_node("EffectsContainer/Buttons/noeffect"), 
							get_node("EffectsContainer/Buttons/tremolo"), 
							get_node("EffectsContainer/Buttons/distortion") ]
onready var trigger_display = get_node("EffectsContainer/Trigger/display")
onready var trigger_pointer = get_node("EffectsContainer/Trigger/pointer")
onready var timer = get_node("EffectsContainer/Trigger/timer")
onready var effect_container = get_node("EffectsContainer/container")
var goto = 0
var next = false
var previous = false

const sensibility_tex = [preload("res://sprites/sensibility_1.png"),
						 preload("res://sprites/sensibility_2.png")]
onready var sensibility_btn = get_node("Home/sensibility")

func _ready():
	set_process(true)
	set_signals()
	pass
	
func _process(delta):
	if sensor == true and wait == false:
		wait = true
		accelerometer_x = Input.get_accelerometer().x
		if accelerometer_x > -.4 and accelerometer_x < .4:
			trigger_pointer.set_rot(deg2rad(-90))
		else:
			trigger_pointer.set_rot( deg2rad((accelerometer_x * 10) -90) )
		deal_with_effects()
		wait = false

func deal_with_effects():
	if call_effect == false:
		if sensibility == 1:
			if accelerometer_x > 7:
				set_new_timer()
				next = true
				previous = false
			elif accelerometer_x < -7:
				set_new_timer()
				next = false
				previous = true
		else:
			if accelerometer_x > 3.5:
				set_new_timer()
				next = true
				previous = false
			elif accelerometer_x < -3.5:
				set_new_timer()
				next = false
				previous = true
			
	else:
		if accelerometer_x < 3 and next == true:
			trigger_toggle_energy(false)
			goto = goto + 1
			if goto > 3:
				goto = 1
			timer.stop()
			if goto == 1:
				_set_effect_1()
			elif goto == 2:
				_set_effect_2()
			elif goto == 3:
				_set_effect_3()
			call_effect = false
		elif accelerometer_x > -3 and previous == true:
			trigger_toggle_energy(false)
			goto = goto - 1
			if goto < 1:
				goto = 3
			timer.stop()
			if goto == 1:
				_set_effect_1()
			elif goto == 2:
				_set_effect_2()
			elif goto == 3:
				_set_effect_3()
			call_effect = false
			
func set_new_timer():
	call_effect = true
	trigger_toggle_energy(true)
	timer.stop()
	timer.set_wait_time(0.4)
	timer.start()
			
func trigger_toggle_energy(boolean):
	if boolean == true:
		trigger_display.set_texture(trigger_display_tex[1])
		trigger_pointer.set_texture(trigger_pointer_tex[1])
	else:
		trigger_display.set_texture(trigger_display_tex[0])
		trigger_pointer.set_texture(trigger_pointer_tex[0])


#GodotBluetooth Methods
func _on_connect_pressed():
	if GlobalScope.bluetooth:
		GlobalScope.bluetooth.getPairedDevices(true)
	else:
		print("Module not initialized!")
	pass

func _set_effect_1():
	#Arduino is programmed to identify this string
	if GlobalScope.bluetooth:
		print("1")
		goto = 1
		GlobalScope.bluetooth.sendData("1")
		for child in effect_container.get_children():
			child.queue_free()
		effect_container.add_child(effect_sprite[0].instance())
	else:
		print("Module not initialized!")
	pass
	
func _set_effect_2():
	#Arduino is programmed to identify this string
	if GlobalScope.bluetooth:
		print("2")
		goto = 2
		GlobalScope.bluetooth.sendData("2")
		for child in effect_container.get_children():
			child.queue_free()
		effect_container.add_child(effect_sprite[1].instance())
	else:
		print("Module not initialized!")
	pass
	
func _set_effect_3():
	#Arduino is programmed to identify this string
	if GlobalScope.bluetooth:
		print("3")
		goto = 3
		GlobalScope.bluetooth.sendData("3")
		for child in effect_container.get_children():
			child.queue_free()
		effect_container.add_child(effect_sprite[2].instance())
	else:
		print("Module not initialized!")
	pass
	
func _on_timeout():
	call_effect = false
	trigger_toggle_energy(false)
	timer.stop()

func _on_connected():
	sensor = true
	anim.play("connected")
	_set_effect_1()
	pass

func _on_disconnected():
	sensor = false
	for child in effect_container.get_children():
		child.queue_free()
	anim.play("disconnected")
	pass
	
func _on_sensibility_toggle():
	if sensibility == 1:
		sensibility_btn.set_normal_texture(sensibility_tex[0])
		sensibility = 0
	else:
		sensibility_btn.set_normal_texture(sensibility_tex[1])
		sensibility = 1
	
#Godot Signals
func set_signals():
	GlobalScope.connect("connected", self, "_on_connected")
	GlobalScope.connect("disconnected", self, "_on_disconnected")
	GlobalScope.connect("data_received", self, "_on_data_received")
	
	timer.connect("timeout", self, "_on_timeout")
	
	btn_connect.connect("pressed", self, "_on_connect_pressed")
	btn_disconnect.connect("pressed", self, "_on_connect_pressed")
	btn_effects[0].connect("pressed", self, "_set_effect_1")
	btn_effects[1].connect("pressed", self, "_set_effect_2")
	btn_effects[2].connect("pressed", self, "_set_effect_3")
	
	sensibility_btn.connect("pressed", self, "_on_sensibility_toggle")
	