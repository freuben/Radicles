// Radicles: call window commands for neural models (nn.ar).
// Part of the Radicles class: the class and its variables are in Radicles.sc.

+ Radicles {
	*raveFunc {var getModelPath, getModels, postModels, postModelsWith, modelArr, modelLoad;
		getModelPath = {arg type=\rave;
			var modelPath;
			case
			{type == \rave} {modelPath = filesPath ++ "/Models/RAVE";}
			{type == \prior} {modelPath = filesPath ++ "/Models/MSPrior";}
			{type == \after} {modelPath = filesPath ++ "/Models/AFTER";}
			{type == \muslat} {modelPath = filesPath ++ "/Models/Music2Latent";}
			{type == \stable} {modelPath = filesPath ++ "/Models/StableAudio";}
			;
			if(modelPath.notNil, {
				modelPath;
			}, {
				"model type not found".postln;
				nil
			});
		};

		getModels = {arg type=\rave;
			var modelPath;
			modelPath = getModelPath.(type);
			if(modelPath.notNil,{
				PathName(modelPath).files.collect{|item| item.fileNameWithoutExtension };
			});
		};

		postModels = {arg type = \rave; getModels.(type).do{|item, index| [index, item].postln;} };

		postModelsWith = {arg type = \rave, find=nil; getModels.(type).do{|item, index| if(item.cs.find(find).notNil, {[index, item].postln;}); } };

		modelArr = nil!10;

		modelLoad = {arg type=\rave, num=0, tsfile=0, action;
			var key, fileName, indexFile, modelNames;
			if((num.isPositive).and(num.isInteger), {
				key = (type.asString ++ num.asString).asSymbol;
				modelNames = getModels.(type);
				if(modelNames.notNil, {
					if(tsfile.isString, {
						indexFile = modelNames.indexOfEqual(tsfile);
						if(indexFile.notNil, {
							fileName = modelNames[indexFile];
						}, {
							"Model not found : wrong file name".radpost;
						});
					}, {
						if(tsfile < (modelNames.size), {
							fileName = modelNames[tsfile];
						}, {
							"Model not found : wrong file index".radpost;
						});
					});
					if(fileName.notNil, {
						{
							"/\/\Loading model...".radpost;
							1.yield;
							"/\/\Model loaded:".radpost;
							("/\/\Model Type: " ++ type.asString.capitalise).radpost;
							("/\/\Model Name: " ++ fileName).radpost;
							("/\/\Model Key: " ++ key).radpost;
						}.fork;
						modelArr[num] = [key, fileName];
						if(action.notNil, {
							NN.load(key, (getModelPath.(type) ++ "/" ++ fileName ++ ".ts"),
								action: action);
						}, {
							NN.load(key, (getModelPath.(type) ++ "/" ++ fileName ++ ".ts"),
								action: _.describe);
						});
						modelNum = num; //changes automatically the key number
					});
				}, {
					"model type not found".radpost;
				});
			}, {
				"model number is wrong - try numbers between 0 and 9".radpost;
			});
		};

		cW.addAll([
			[\rave, [\str], { postModels.(\rave); }, "post rave models"],
			[\prior, [\str], { postModels.(\prior); }, "post prior models"],
			[\after, [\str], { postModels.(\after); }, "post after models"],
			[\muslat, [\str], { postModels.(\muslat); }, "post music2latent models"],
			[\stable, [\str], { postModels.(\stable); }, "post stable audio models"],
			[\rave, [\str, \str], {|str1, str2| postModelsWith.(\rave, str2.asString); }, "post rave models with find"],
			[\prior, [\str, \str], {|str1, str2| postModelsWith.(\prior, str2.asString); }, "post prior models with find"],
			[\after, [\str, \str], {|str1, str2| postModelsWith.(\after, str2.asString); }, "post prior models with find"],
			[\models, [\str], { modelArr.do({|item, index|
				if(item.notNil, {([index+1] ++ item).radpost}); }); },
			"post loaded models"],
			[\modelNum, [\str], { ("/\/\Current model number: " ++ modelNum).radpost; },
				"change current model number"],
			[\modelNum, [\str, \num], {|str, num| modelNum = num; },
				"change current model number"],
			[\modelInfo, [\str], { NN.describeAll },
				"post info about all loaded models"],
			[\modelInfo, [\str, \str], {|str1, str2| NN(str2.asSymbol).describe },
				"post info about this model key"],
			[\rave, [\str, \num, \num], {|str, num1, num2| modelLoad.(\rave,num1,num2);},
				"load rave model with index"],
			[\rave, [\str, \num, \str], {|str1, num, str2| modelLoad.(\rave,num,str2.asString);},
				"load rave model with name"],
			[\prior, [\str, \num, \num], {|str, num1, num2| modelLoad.(\prior,num1,num2);},
				"load rave model with index"],
			[\prior, [\str, \num, \str], {|str1, num, str2| modelLoad.(\prior,num,str2.asString);},
				"load rave model with name"],
			[\after, [\str, \num, \num], {|str, num1, num2| modelLoad.(\after,num1,num2);},
				"load rave model with index"],
			[\after, [\str, \num, \str], {|str1, num, str2| modelLoad.(\after,num,str2.asString);},
				"load rave model with name"],
			[\muslat, [\str, \num, \num], {|str, num1, num2| modelLoad.(\muslat,num1,num2);},
				"load music2latent model with index"],
			[\muslat, [\str, \num, \str], {|str1, num, str2| modelLoad.(\muslat,num,str2.asString);},
				"load music2latent model with name"],
			[\stable, [\str, \num, \num], {|str, num1, num2| modelLoad.(\stable,num1,num2);},
				"load stable audio model with index"],
			[\stable, [\str, \num, \str], {|str1, num, str2| modelLoad.(\stable,num,str2.asString);},
				"load stable audio model with name"],
			[\raveins, [\str], {|str| cW.callFunc("(in 1 1 & in 2 2 & in 3 3 & in 4 4 & blk 5 audioins)" )},
				"rave inputs"],
			[\brave1, [\str, \num, \num], {|str, num1, num2|
				var raveNum;
				raveNum = num1-1;
				modelLoad.(\rave,raveNum,num2, action: {
					if(~panArr.isNil, {~panArr = 0!Radicles.aZ.busCount;});
					Radicles.aZ.setFx(\bus, num1, 3, \nntImprov,
						[\pan, ~panArr[raveNum]]);
				});
			},
			"load rave model and fx in bus number"],
			[\bafter1, [\str, \num, \num], {|str, num1, num2|
				var raveNum;
				raveNum = num1-1;
				modelLoad.(\after,raveNum,num2, action: {
					if(~panArr.isNil, {~panArr = 0!Radicles.aZ.busCount;});
					Radicles.aZ.setFx(\bus, num1, 3, \afterImprov,
						[\pan, ~panArr[raveNum]]);
				});
			},
			"load after model and fx in bus number"],
			[\bmuslat1, [\str, \num, \num], {|str, num1, num2|
				var raveNum;
				raveNum = num1-1;
				modelLoad.(\muslat,raveNum,num2, action: {
					if(~panArr.isNil, {~panArr = 0!Radicles.aZ.busCount;});
					Radicles.aZ.setFx(\bus, num1, 3, \nntImprov64,
						[\pan, ~panArr[raveNum]]);
				});
			},
			"load muslat model and fx in bus number"],
			[\brave, [\str, \num, \num], {|str, num1, num2|
				var raveNum;
				raveNum = num1-1;
				modelLoad.(\rave,raveNum,num2, action: {
					Radicles.aZ.setFx(\bus, num1, 3, \nntImprov2);
				});
			},
			"load rave model fake stereo and fx in bus number"],

			[\bafter, [\str, \num, \num], {|str, num1, num2|
				var raveNum;
				raveNum = num1-1;
				modelLoad.(\after,raveNum,num2, action: {
					Radicles.aZ.setFx(\bus, num1, 3, \afterImprov2);
				});
			},
			"load after model fake stereo and fx in bus number"],

			[\brave1, [\str, \arr, \num], {|str, arr1, num2|
				var raveNum, cond1;
				cond1 = Condition.new(false);

				arr1.postln;
				{
					arr1.do{|item|
						raveNum = item-1;

						if(~panArr.isNil, {~panArr = 0!Radicles.aZ.busCount;});

						modelLoad.(\rave,raveNum,num2, action: {
							Radicles.aZ.setFx(\bus, item, 3, \nntImprov,
								[\pan, ~panArr[raveNum]], action:
								{
									cond1.test = true;
									cond1.signal;
							})
						}
						);

						cond1.test = false;
						cond1.wait;
					};
				}.fork;


			},
			"load array of rave models and fx in bus number"],

			[\bafter1, [\str, \arr, \num], {|str, arr1, num2|
				var raveNum, cond1;
				cond1 = Condition.new(false);

				arr1.postln;
				{
					arr1.do{|item|
						raveNum = item-1;

						if(~panArr.isNil, {~panArr = 0!Radicles.aZ.busCount;});

						modelLoad.(\after,raveNum,num2, action: {
							Radicles.aZ.setFx(\bus, item, 3, \afterImprov,
								[\pan, ~panArr[raveNum]], action:
								{
									cond1.test = true;
									cond1.signal;
							})
						}
						);

						cond1.test = false;
						cond1.wait;
					};
				}.fork;
			},
			"load array of after models and fx in bus number"],

			[\brave, [\str, \arr, \num], {|str, arr1, num2|
				var raveNum, cond1;
				cond1 = Condition.new(false);

				arr1.postln;
				{
					arr1.do{|item|
						raveNum = item-1;

						modelLoad.(\rave,raveNum,num2, action: {
							Radicles.aZ.setFx(\bus, item, 3, \nntImprov2, action:
								{
									cond1.test = true;
									cond1.signal;
							})
						}
						);

						cond1.test = false;
						cond1.wait;
					};
				}.fork;


			},
			"load array of fake stereo rave models and fx in bus number"],

			[\bafter, [\str, \arr, \num], {|str, arr1, num2|
				var raveNum, cond1;
				cond1 = Condition.new(false);

				arr1.postln;
				{
					arr1.do{|item|
						raveNum = item-1;

						modelLoad.(\after,raveNum,num2, action: {
							Radicles.aZ.setFx(\bus, item, 3, \afterImprov2, action:
								{
									cond1.test = true;
									cond1.signal;
							})
						}
						);

						cond1.test = false;
						cond1.wait;
					};
				}.fork;


			},
			"load array of fake stereo after models and fx in bus number"],

			[\braven, [\str, \num, \num], {|str, num1, num2|
				var raveNum;
				raveNum = num1-1;
				modelLoad.(\rave,raveNum,num2, action: {
					Radicles.aZ.setFx(\bus, num1, 3, \nnt);
					/*cW.callFunc( ("fx b " ++ num1 ++ " 3 nntImprov") );*/
				});
			},
			"load rave model and fx in bus number"],

			[\braven, [\str, \arr, \num], {|str, arr1, num2|
				var raveNum, cond1;
				cond1 = Condition.new(false);

				arr1.postln;
				{
					arr1.do{|item|
						raveNum = item-1;

						modelLoad.(\rave,raveNum,num2, action: {
							Radicles.aZ.setFx(\bus, item, 3, \nnt, action:
								{
									cond1.test = true;
									cond1.signal;
							})
						}
						);

						cond1.test = false;
						cond1.wait;
					};
				}.fork;


			},
			"load array of rave models and fx in bus number"],

		]);


	}
}
