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
            url: "http://localhost:8080/gwt-console-server/rs/process/definition/"+ id + "/instances",
            method: 'GET',
            success: function(response){
    	   		try {   	   			
    	   			if(response.responseText && response.responseText.length > 0) {
	   	   				var instancesjson = response.responseText.evalJSON();
    	   				var instancesobj = instancesjson["instances"];
 
    		        	for(var i=0;i<instancesobj.length;i++){
    	   					var toapplyobj = instancesobj[i];
	    	   				//alert(instancesobj[i].id);
	    	   				//alert(instancesobj[i].rootToken.currentNodeName);
    	   					this.checkNodes(toapplyobj);
    	   					//this.checkNodes.bind(this, instancesobj[i])
    	   				}
	   	   			} else {
	    	   				Ext.Msg.alert('Invalid Instances data1.');
	    	   			}
	    	   		} catch(e) {
	    	   			Ext.Msg.alert('Error applying Instances data2:\n' + e);
	    	   		}
	            }.bind(this),
	            failure: function(){
	            	Ext.Msg.alert('Error applying Instances data.');
	            }
         	});		
	},

	checkNodes: function(toapplyobj) {
		var canvas = this.facade.getCanvas();

		ORYX.EDITOR._canvas.getChildNodes().each((function(child) {
				if(child.properties["oryx-name"] == toapplyobj.rootToken.currentNodeName){
					//alert(child.properties["oryx-name"]);
					child.setProperty("oryx-bordercolor", "#888888");
    				child.setProperty("oryx-bgcolor", "#CCEEFF");
    		   		child.setSelectable(false);
		    		child.setMovable(false);
		    		child.refresh();
/*    				if(child.getChildren().size() > 0) {
						for (var i = 0; i < shape.getChildren().size(); i++) {
							if(child.getChildren()[i] instanceof ORYX.Core.Node || child.getChildren()[i] instanceof ORYX.Core.Node) {
								this.lockShape(shape.getChildren()[i]);
							}
						}
    				}*/
	    	}
    	}).bind(this));
		
		/*var childthemestr = themeObj[childgroup];
		if(childthemestr && child.properties["oryx-isselectable"] != "false") { 
			var themestrparts = childthemestr.split("|");
			child.setProperty("oryx-bgcolor", themestrparts[0]);
			child.setProperty("oryx-bordercolor", themestrparts[1]);
			child.setProperty("oryx-fontcolor", themestrparts[2]);
			child.refresh();
		}
		if(child.getChildNodes().size() > 0) {
			for (var i = 0; i < child.getChildNodes().size(); i++) {
				this.applyThemeToNodes(child.getChildNodes()[i], themeObj);
			}
		}*/
	}
});