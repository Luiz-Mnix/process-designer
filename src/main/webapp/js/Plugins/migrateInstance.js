if (!ORYX.Plugins) 
    ORYX.Plugins = new Object();

ORYX.Plugins.MigrateInstance = ORYX.Plugins.AbstractPlugin.extend({
	construct: function(facade){
		this.facade = facade;

		this.facade.offer({
			'name': 'MigrateInstance',
			'functionality': this.migrateinstance.bind(this),
			'group': ORYX.I18N.checkCompliance.group,
			'icon': ORYX.PATH + "images/migrateInstance.png",
			'description': 'Migrate Instances',
			'index': 3,
            'toggle': true,
			'minShape': 0,
			'maxShape': 0
		});
	},
	migrateinstance: function() {
			var canvas = this.facade.getCanvas();
		
			var id = canvas.properties["oryx-id"];
			//alert(id);	
			
		Ext.Ajax.request({
            url: window.location.protocol + "//" + window.location.host +"/gwt-console-server/rs/process/instance/22/delete",
            method: 'POST',
            success: function(response){
    	   		try {   	   			
    	   			if(response.responseText && response.responseText.length > 0) {
	   	   					Ext.Msg.alert("Success : " + response);
 	   	   			} else {
	    	   				Ext.Msg.alert("No Success : " + response);
	    	   			}
	    	   		} catch(e) {
	    	   			Ext.Msg.alert('Error applying Instances data1:\n' + e);
	    	   		}
	            }.bind(this),
            failure: function(){
            	Ext.Msg.alert('Error applying Instances data2.');
            }
     	});			
	}
});