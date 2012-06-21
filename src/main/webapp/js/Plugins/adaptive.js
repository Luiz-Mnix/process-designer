if (!ORYX.Plugins) 
    ORYX.Plugins = new Object();

ORYX.Plugins.Adaptive = ORYX.Plugins.AbstractPlugin.extend({
	construct: function(facade){
		this.facade = facade;

		this.facade.offer({
			'name': 'Adaptive',
			'functionality': this.adaptive.bind(this),
			'group': ORYX.I18N.checkCompliance.group,
			'icon': ORYX.PATH + "images/play.png",
			'description': 'Get executed tasks',
			'index': 2,
            'toggle': true,
			'minShape': 0,
			'maxShape': 0
		});
	},
	adaptive: function() {
			var canvas = this.facade.getCanvas();
		
			var id = canvas.properties["oryx-id"];
			//alert(id);	
			
			Ext.Ajax.request({
            url: window.location.protocol + "//" + window.location.host +"/gwt-console-server/rs/process/definition/"+ id + "/instances",
            method: 'GET',
            success: function(response){
    	   		try {   	   			
    	   			if(response.responseText && response.responseText.length > 0) {
	   	   				var instancesjson = response.responseText.evalJSON();
    	   				var instancesobj = instancesjson["instances"];
    	   				if(instancesobj[0] != null){
	   	   					this.checkNodes(instancesobj[0]);
	   	   				} else {
	    	   				Ext.Msg.alert('No Instances available for '+id);	   	   					
	   	   				}
 	   	   			} else {
	    	   				Ext.Msg.alert('Invalid Instances data.');
	    	   			}
	    	   		} catch(e) {
	    	   			Ext.Msg.alert('Error applying Instances data1:\n' + e);
	    	   		}
	            }.bind(this),
	            failure: function(){
	            	Ext.Msg.alert('Error applying Instances data2.');
	            }
         	});	
         	
         		
	},

	checkNodes: function(instances) {
		if(instances){
			ORYX.EDITOR._canvas.getChildren().each((function(child) {
				this.lockShape(child);
				if(child.properties["oryx-name"] == instances.rootToken.currentNodeName){
					throw $break;
	    		}
	    	}).bind(this));
	   }
	},
	lockShape: function(shape) {
		if(shape){
			shape.setSelectable(false);
    		shape.setMovable(false);
    		if(shape instanceof ORYX.Core.Node || shape instanceof ORYX.Core.Edge) {
    			shape.setProperty("oryx-bordercolor", "#888888");
    			shape.setProperty("oryx-bgcolor", "#CCEEFF");
    		}
    		//shape.setProperty("oryx-isselectable", "false");
    		shape.refresh();
			if(shape.getChildren().size() > 0) {
				for (var i = 0; i < shape.getChildren().size(); i++) {
					if(shape.getChildren()[i] instanceof ORYX.Core.Node || shape.getChildren()[i] instanceof ORYX.Core.Node) {
						this.lockShape(shape.getChildren()[i]);
					}
				}
			}
	    }
	  }
});