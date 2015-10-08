module.exports = function(grunt) {
 
    // Project configuration.
    grunt.initConfig({
        pkg: grunt.file.readJSON('package.json'),
        //uglify 
        uglify: {
            options: {
              banner: '/* <%= grunt.template.today("yyyy-mm-dd") %> */ ',
              preserveComments: 'some'
            },
            build: {
                src: 'dist/scouter-all.js',
                dest: 'dist/scouter-all.min.js'
            }
        },
        concat:{
            basic: {
                src: ['script/src/error/scouter-script-error.js',
                      'script/src/timing/*.js'],
                dest: 'dist/scouter-all.js'
            }
        }
    });
 
    // Load the plugin that provides the "uglify", "concat" tasks.
    grunt.loadNpmTasks('grunt-contrib-uglify');
    grunt.loadNpmTasks('grunt-contrib-concat');
 
    // Default task(s).
    grunt.registerTask('default', ['concat', 'uglify']); 
 
};
