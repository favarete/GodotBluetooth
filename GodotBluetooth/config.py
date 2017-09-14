def can_build(plat):
	return plat=="android"

def configure(env):
	if (env['platform'] == 'android'):
		env.android_add_to_permissions("android/AndroidPermissionsChunk.xml")
		env.android_add_java_dir("android/src")
		env.android_add_dependency("compile 'com.android.support:appcompat-v7:23.1.1'")