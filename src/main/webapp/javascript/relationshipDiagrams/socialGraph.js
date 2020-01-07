//Social graph global API (used in individuals.jsp)
function setupSocialGraph(individualID) {
    let focusedScale = 1.25;
    let sg = new SocialGraph(individualID, "#socialDiagram", focusedScale);
    sg.applySocialData();
}

//Tree-like graph displaying social and familial relationships for a species
class SocialGraph extends ForceLayoutAbstract {
    constructor(individualID, containerId, focusedScale) {
	super(individualID, containerId, focusedScale);
    }

    //Wrapper function to gather species data from the Wildbook DB and generate a graph
    applySocialData() {
	this.parser.parseJSON(this.id, (nodes, links) => this.graphSocialData(nodes, links));
    }

    //Generate a social graph
    graphSocialData(nodes, links) {
	if (nodes.length > 0) {
	    //Create graph w/ forces
	    this.setupGraph(links, nodes);
	    this.updateGraph(links, nodes);
	}
	else this.showTable("#socialDiagram", "#communityTable");
    }
}
