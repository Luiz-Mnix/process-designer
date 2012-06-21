if (!ORYX.Plugins) 
    ORYX.Plugins = new Object();

ORYX.Plugins.MigrateInstance = ORYX.Plugins.AbstractPlugin.extend({
	construct: function(facade){
		this.facade = facade;

		this.facade.offer({
			'name': 'MigateInstance',
			'functionality': this.migrateInstance.bind(this),
			'group': ORYX.I18N.checkCompliance.group,
			'icon': ORYX.PATH + "images/migrateInstance.png",
			'description': 'Migrate Instances',
			'index': 3,
            'toggle': true,
			'minShape': 0,
			'maxShape': 0
		});
	},
	migrateInstance: function() {
			Ext.Ajax.request({
            url: ORYX.PATH + "compliancecheck",
            method: 'POST',
            success: function(request){
    	   		try {
    	   			diffLoadMask.hide();
    	   			this._showProcessDiffDialog(request.responseText);
    	   		} catch(e) {
    	   			diffLoadMask.hide();
    	   			Ext.Msg.alert("Failed to retrieve process version information:\n" + e);
    	   		}
            }.createDelegate(this),
            failure: function(){
            	diffLoadMask.hide();
            	Ext.Msg.alert("Failed to retrieve process version information.");
            },
            params: {
            	action: 'getProcessName',
            	profile: ORYX.PROFILE,
            	uuid : ORYX.UUID
            }
        });	
	}
});